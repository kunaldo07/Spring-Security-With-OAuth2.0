package com.kunaldo.springsecurityclient.event.listener;

import com.kunaldo.springsecurityclient.entity.User;
import com.kunaldo.springsecurityclient.event.RegistrationCompleteEvent;
import com.kunaldo.springsecurityclient.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class RegistrationCompleteEventListener implements ApplicationListener<RegistrationCompleteEvent> {

    @Autowired
    private UserService userService;

    @Override
    public void onApplicationEvent(RegistrationCompleteEvent event) {
        // Create the verification token for the User (URL)
        // Send mail to the user
        // when the user clicks on this url then the user will automatically gets redirected to the main app
        // when the user click on this url we will match the token in that url with the token in the database

        User user = event.getUser();
        String token = UUID.randomUUID().toString();
        userService.saveVerificationForUser(token,user);

        // Send mail to the user for the link verification
        String url = event.getApplicationUrl() + "/verifyRegistration?token"+token;

        // sendVerificationEmail()
        log.info("Click the info to verify your account: {}", url);

    }
}
