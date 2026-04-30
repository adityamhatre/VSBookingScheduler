import requests

url = 'http://localhost:80/uploadReleaseApk'

data = {'versionCode': '15'}


files = {'apk': open('D:\\Android Studio Projects\\VSBookingSchedular\\app\\release\\app-release.apk', 'rb')}

r = requests.post(url, json=data, data=open('D:\\Android Studio Projects\\VSBookingSchedular\\app\\release\\app-release.apk', 'rb'))
