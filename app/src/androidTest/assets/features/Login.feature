Feature: As a runner, I want to be able to log-in to the application, to be able to start my trip
  and manage the shipments delivery

Scenario: Runner login in with successful credentials
 Given Runner at the login page
 When He inserts his credentials successfully
 Then He will be logged to the app successfully

Scenario Outline: Unsuccessfull runner login to the App
 Given Runner at the login page
 When Runner login in using <invalid credentials>
 Then This <error message> is displayed

 Examples:
   |invalid credentials      |error message                                            |
   |wrong credentials        |برجاء التأكد من كود المندوب او كلمة السر و اعادة المحاولة|
   |deleted user credentials |برجاء التأكد من كود المندوب او كلمة السر و اعادة المحاولة|
   |missing credentials      |                برجاء ادخال كلا من كود المندوب و كلمة السر|
   |inactive user credentials|     هذا المستخدم غير مفعل برجاء الرجوع الي الجهة المعنية|
