#!/usr/bin/env bash
# ============================================================================
# scripts/deploy/bootstrap.sh — One-shot Ubuntu server provisioning
# ============================================================================
# Run as:    root, ONCE on a fresh Ubuntu 22.04 (jammy) or 24.04 (noble) LTS.
# Purpose:   Install Docker Engine + Compose plugin, configure firewall,
#            fail2ban, unattended-upgrades; create a non-root `deploy` user
#            with Docker access; harden SSH.
#
# Usage:     bash bootstrap.sh [path-to-ssh-public-key]
#
# After this script finishes:
#   - SSH into the server as `deploy@<ip>` (password OR your uploaded key).
#   - Run scripts/deploy/deploy.sh from the cloned repo as the deploy user.
#
# Idempotent: re-running is safe; apt/docker steps skip if already installed.
# ============================================================================
set -euo pipefail

# ---- Constants --------------------------------------------------------------
readonly SUPPORTED_CODENAMES=(jammy noble)
readonly DEPLOY_USER="deploy"
readonly SSH_PUBLIC_KEY="${1:-}"

# ---- Helpers ----------------------------------------------------------------
log()  { printf '\033[1;34m[bootstrap]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[bootstrap][warn]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[bootstrap][fatal]\033[0m %s\n' "$*" >&2; exit 1; }

require_root() {
    if [[ "$EUID" -ne 0 ]]; then
        die "must run as root (try: sudo bash bootstrap.sh)"
    fi
}

# ---- 1. Detect OS ----------------------------------------------------------
require_root

if ! command -v lsb_release >/dev/null 2>&1; then
    die "lsb_release not found; this script targets Ubuntu 22.04/24.04 LTS"
fi

. /etc/os-release
CODENAME="${UBUNTU_CODENAME:-$(lsb_release -cs)}"
if [[ ! " ${SUPPORTED_CODENAMES[*]} " =~ " ${CODENAME} " ]]; then
    die "Unsupported Ubuntu '${CODENAME}'. Supported: ${SUPPORTED_CODENAMES[*]}"
fi
log "Detected Ubuntu ${VERSION:-?} (${CODENAME})"

# ---- 2. Base packages ------------------------------------------------------
log "Installing base packages..."
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -yqq --no-install-recommends \
    ca-certificates curl gnupg lsb-release \
    ufw fail2ban unattended-upgrades tzdata chrony \
    openssl git

# ---- 3. Docker Engine + Compose plugin (from Docker's official apt repo) --
if ! command -v docker >/dev/null 2>&1; then
    log "Adding Docker apt repo and installing docker-ce + compose plugin..."
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL "https://download.docker.com/linux/ubuntu/gpg" \
        | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${CODENAME} stable" \
        > /etc/apt/sources.list.d/docker.list
    apt-get update -qq
    apt-get install -yqq --no-install-recommends \
        docker-ce docker-ce-cli containerd.io \
        docker-buildx-plugin docker-compose-plugin
else
    log "Docker already installed: $(docker --version)"
fi

# ---- 4. Timezone ------------------------------------------------------------
log "Setting timezone to Asia/Shanghai..."
timedatectl set-timezone Asia/Shanghai
ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# ---- 5. Docker daemon config (log rotation + live-restore) ---------------
log "Writing /etc/docker/daemon.json (log rotation, live-restore)..."
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "live-restore": true,
  "default-runtime": "runc"
}
EOF
systemctl restart docker

# ---- 6. UFW firewall --------------------------------------------------------
log "Configuring UFW (default deny; allow 22/8080/9000)..."
ufw --force reset
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment 'SSH'
ufw allow 8080/tcp comment 'Smart Watch HTTP'
ufw allow 9000/tcp comment 'Smart Watch Netty device comm'
# Port 9090 (TCP echo) is intentionally NOT opened. TcpSocketServer is a
# debug-only service. If you need it, run: sudo ufw allow 9090/tcp
ufw --force enable

# ---- 7. fail2ban ------------------------------------------------------------
log "Configuring fail2ban (SSH jail)..."
cat > /etc/fail2ban/jail.local <<'EOF'
[DEFAULT]
backend = systemd

[sshd]
enabled = true
port = ssh
maxretry = 5
findtime = 10m
bantime = 1h
EOF
systemctl enable --now fail2ban

# ---- 8. unattended-upgrades -------------------------------------------------
log "Enabling unattended security upgrades..."
cat > /etc/apt/apt.conf.d/20auto-upgrades <<'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
APT::Periodic::AutocleanInterval "7";
Unattended-Upgrade::Remove-Unused-Dependencies "true";
Unattended-Upgrade::Automatic-Reboot "false";
EOF
dpkg-reconfigure -f noninteractive unattended-upgrades >/dev/null 2>&1 || true

# ---- 9. deploy user ---------------------------------------------------------
if ! id -u "$DEPLOY_USER" >/dev/null 2>&1; then
    log "Creating user '$DEPLOY_USER'..."
    useradd -m -s /bin/bash -d "/home/$DEPLOY_USER" "$DEPLOY_USER"
    usermod -aG docker "$DEPLOY_USER"
else
    log "User '$DEPLOY_USER' already exists; ensuring docker group..."
    usermod -aG docker "$DEPLOY_USER"
fi

# SSH key for deploy user
install -d -m 0700 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "/home/$DEPLOY_USER/.ssh"
touch "/home/$DEPLOY_USER/.ssh/authorized_keys"
chown "$DEPLOY_USER:$DEPLOY_USER" "/home/$DEPLOY_USER/.ssh/authorized_keys"
chmod 0600 "/home/$DEPLOY_USER/.ssh/authorized_keys"

if [[ -n "$SSH_PUBLIC_KEY" && -f "$SSH_PUBLIC_KEY" ]]; then
    log "Installing SSH public key from $SSH_PUBLIC_KEY..."
    cat "$SSH_PUBLIC_KEY" >> "/home/$DEPLOY_USER/.ssh/authorized_keys"
    chmod 0600 "/home/$DEPLOY_USER/.ssh/authorized_keys"
    warn "Public key installed. After verifying you can log in with the key,"
    warn "disable password auth with: sudo sed -i 's/^#\\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config && sudo systemctl reload ssh"
else
    warn "No SSH public key supplied. PasswordAuthentication is still enabled."
    warn "Upload one with: ssh-copy-id ${DEPLOY_USER}@<this-server-ip>"
    warn "Then disable passwords: see warning above."
fi

# ---- 10. Passwordless sudo for deploy (limited to apt + systemctl) --------
log "Granting limited passwordless sudo to '$DEPLOY_USER'..."
cat > /etc/sudoers.d/"$DEPLOY_USER" <<EOF
${DEPLOY_USER} ALL=(ALL) NOPASSWD: /usr/bin/apt-get, /usr/bin/apt, /usr/bin/systemctl, /usr/sbin/ufw, /usr/sbin/fail2ban-client
EOF
chmod 0440 /etc/sudoers.d/"$DEPLOY_USER"
visudo -c -f /etc/sudoers.d/"$DEPLOY_USER" >/dev/null

# ---- 11. SSH hardening ------------------------------------------------------
log "Hardening /etc/ssh/sshd_config..."
SSHD_CONF=/etc/ssh/sshd_config
cp -a "$SSHD_CONF" "${SSHD_CONF}.bak.$(date +%s)"
declare -A SSH_SETTINGS=(
    ["PermitRootLogin"]="no"
    ["PubkeyAuthentication"]="yes"
    ["PasswordAuthentication"]="yes"
    ["X11Forwarding"]="no"
    ["AllowUsers"]="$DEPLOY_USER"
)
for key in "${!SSH_SETTINGS[@]}"; do
    value="${SSH_SETTINGS[$key]}"
    if grep -qE "^\s*#?\s*${key}\b" "$SSHD_CONF"; then
        sed -i "s|^\s*#\?\s*${key}\b.*|${key} ${value}|" "$SSHD_CONF"
    else
        printf '\n%s %s\n' "$key" "$value" >> "$SSHD_CONF"
    fi
done
sshd -t && systemctl reload ssh

# ---- 12. Summary -----------------------------------------------------------
PUBLIC_IP="$(curl -fsS --max-time 5 https://api.ipify.org 2>/dev/null || echo '<public-ip>')"
cat <<EOF

============================================================================
Bootstrap complete!
============================================================================
  OS:           Ubuntu ${VERSION:-?} (${CODENAME})
  Public IP:    ${PUBLIC_IP}
  Deploy user:  ${DEPLOY_USER} (in docker group, sudo for apt/systemctl/ufw)
  UFW open:     22/tcp (SSH), 8080/tcp (HTTP), 9000/tcp (device)
  UFW closed:   9090/tcp (intentional — TCP echo is dev-only)
  Docker:       $(docker --version 2>/dev/null || echo 'NOT installed')
  Compose:      \$(docker compose version 2>/dev/null || echo 'NOT installed')
  Timezone:     $(timedatectl show -p Timezone --value)
  Log rotation: json-file, 10m x 3
  Auto updates: enabled (unattended-upgrades)

NEXT STEPS (run from your laptop):
  1. ssh ${DEPLOY_USER}@${PUBLIC_IP}
  2. Clone the repo and run scripts/deploy/deploy.sh

REMINDER: Disable SSH password auth as soon as you've verified key-based login:
  sudo sed -i 's/^#\\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
  sudo systemctl reload ssh
============================================================================
EOF
