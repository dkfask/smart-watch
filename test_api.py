import requests

# 调用报警API
try:
    response = requests.get('http://localhost:8080/api/alarms')
    response.raise_for_status()  # 检查请求是否成功
    
    # 打印响应状态码
    print(f"Response Status Code: {response.status_code}")
    
    # 打印响应内容
    print("Response Content:")
    print(response.json())
except requests.exceptions.RequestException as e:
    print(f"Error: {e}")
