package com.kunaldo.springsecurityclient.controller;

import com.kunaldo.springsecurityclient.entity.User;
import com.kunaldo.springsecurityclient.entity.VerificationToken;
import com.kunaldo.springsecurityclient.event.RegistrationCompleteEvent;
import com.kunaldo.springsecurityclient.model.PasswordModel;
import com.kunaldo.springsecurityclient.model.UserModel;
import com.kunaldo.springsecurityclient.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@Slf4j
public class RegistrationController {

    @Autowired
    private UserService userService;

    // to know the user has logged in and then we can send him the verification email
    @Autowired
    private ApplicationEventPublisher publisher;
    @PostMapping("/register")
    public String registerUser(@RequestBody UserModel userModel, final HttpServletRequest request) {
        // save the user in the database
        User user = userService.registerUser(userModel);
        // event to send the token to the user in form of an email for verification
        publisher.publishEvent(new RegistrationCompleteEvent(
                user,
                applicationUrl(request)
        ));
        return "Success";
    }

    @GetMapping("/verifyRegistration")
    public String verifyRegistration(@RequestParam("token") String token) {
        String result = userService.validateVerificationToken(token);
        if (result.equalsIgnoreCase("valid")){
            return "User verified Successfully";
        }
        return "Bad User";
    }

    // resentVerificationToken method is use to generate a new token and link for the verification
    @GetMapping("/resentVerifyToken")
    public String resentVerificationToken(@RequestParam("token") String oldToken, HttpServletRequest request) {

        VerificationToken verificationToken = userService.generateNewVerificationToken(oldToken);
        User user = verificationToken.getUser();
        resentVerificationTokenMail(user,applicationUrl(request),verificationToken);
        return "Verification Link Sent";
    }

    @PostMapping("/resetPassword")
    public String resetPassword(@RequestBody PasswordModel passwordModel, HttpServletRequest request) {
        // checking if the user email Id is present in our system or not
        // if its present then create and send a password token through email
        User user = userService.findUserByEmail(passwordModel.getEmail());
        String url = "";
        if(user!=null) {
            String token = UUID.randomUUID().toString();
            userService.createPasswordResetTokenForUser(user,token);
            url = passwordResetTokenMail(user,applicationUrl(request), token);
        }
        return url;
    }

    @PostMapping("/savePassword")
    public String savePassword(@RequestParam("token") String token,@RequestBody PasswordModel passwordModel) {
        String result = userService.validatePasswordResetToken(token);
        if(!result.equalsIgnoreCase("valid")) {
            return "Invalid Token";
        }
        Optional<User> user = userService.getUserByPasswordResetToken(token);
        if(user.isPresent()) {
            userService.changePassword(user.get(), passwordModel.getNewPassword());
            return "Password Reset Successfully";
        } else {
            return "Invalid Token";
        }
    }

    @PostMapping("/changePassword")
    public String changePassword(@RequestBody PasswordModel passwordModel) {
        User user = userService.findUserByEmail(passwordModel.getEmail());
        if (!userService.checkIfValidOldPassword(user,passwordModel.getOldPassword())) {
            return "Invalid Old Password";
        }

        // Save New Password
        userService.changePassword(user,passwordModel.getNewPassword());
        return "Password Changed Successfully";
    }

    private String passwordResetTokenMail(User user, String applicationUrl, String token) {
        String url = applicationUrl + "/savePassword?token="+ token;
        // sendVerificationEmail()
        log.info("Click the info to reset your password: {}", url);
        return url;
    }

    private void resentVerificationTokenMail(User user, String applicationUrl,VerificationToken verificationToken) {
        String url = applicationUrl + "/verifyRegistration?token="+verificationToken.getToken();
        // sendVerificationEmail()
        log.info("Click the info to verify your account: {}", url);
    }


    //create and return the url
    private String applicationUrl(HttpServletRequest request) {
        return "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }
}
