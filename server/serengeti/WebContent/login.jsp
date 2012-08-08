<%@ page pageEncoding="UTF-8" %>

<html>
  <head>
    <title>Welcome to Serengeti</title>
  </head>

  <body onload="document.f.j_username.focus();">
    <h1>Welcome to Serengeti</h1>

    <p>Locale is: <%= request.getLocale() %></p>

    <form name="f" action="j_spring_security_check" method="POST">
      <table>
        <tr><td>Username: </td><td><input type='text' name='j_username'></td></tr>
        <tr><td>Password: </td><td><input type='password' name='j_password'></td></tr>
      </table>
      <input type="submit" value="login" />
    </form>
  </body>
</html>
