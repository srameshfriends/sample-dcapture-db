package sample.dcapture.sql.model;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class User {
    private int id;
    private String email, userName, fullName, password, confirmPassword;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    @Override
    public String toString() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("email", email == null ? "" : email);
        builder.add("userName", userName == null ? "" : userName);
        builder.add("fullName", fullName == null ? "" : fullName);
        builder.add("password", password == null ? "" : password);
        builder.add("confirmPassword", confirmPassword == null ? "" : confirmPassword);
        return builder.build().toString();
    }
}
