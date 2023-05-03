package com.kunaldo.springsecurityclient.service;

import com.kunaldo.springsecurityclient.entity.PasswordResetToken;
import com.kunaldo.springsecurityclient.entity.User;
import com.kunaldo.springsecurityclient.entity.VerificationToken;
import com.kunaldo.springsecurityclient.model.UserModel;
import com.kunaldo.springsecurityclient.repository.PasswordResetTokenRepository;
import com.kunaldo.springsecurityclient.repository.UserRepository;
import com.kunaldo.springsecurityclient.repository.VerificationTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService{

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public User registerUser(UserModel userModel) {
        User user = new User();
        user.setEmail(userModel.getEmail());
        user.setFirstName(userModel.getFirstName());
        user.setLastName(userModel.getLastName());
        user.setRole("USER");
        user.setPassword(passwordEncoder.encode(userModel.getPassword()));
        userRepository.save(user);
        return user;
    }

    // Now user and token have been saved into the database
    @Override
    public void saveVerificationForUser(String token, User user) {
        VerificationToken verificationToken = new VerificationToken(user,token);
        verificationTokenRepository.save(verificationToken);
    }

    @Override
    public String validateVerificationToken(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token);
        // checking if the token is present in the database oor not, if its present then its valid
        if (verificationToken == null) {
            // Invalid Token
            return "invalid";
        }
        User user = verificationToken.getUser();
        Calendar calendar = Calendar.getInstance();

        // Checking if that token is expired or not
        if ((verificationToken.getExpirationTime().getTime() - calendar.getTime().getTime()) <= 0) {
            // Now the token has expired, we should delete that token now
            verificationTokenRepository.delete(verificationToken);
            // Token is expired
            return "expired";
        }

        // otherwise we are enabling the user and saving it in repository
        user.setEnabled(true);
        userRepository.save(user);
        // Token is valid
        return "valid";
    }

    @Override
    public VerificationToken generateNewVerificationToken(String oldToken) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(oldToken);
        // Setting the new token inplace of the old one
        verificationToken.setToken(UUID.randomUUID().toString());
        verificationTokenRepository.save(verificationToken);
        return verificationToken;
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void createPasswordResetTokenForUser(User user, String token) {
        PasswordResetToken passwordResetToken = new PasswordResetToken(user,token);
        passwordResetTokenRepository.save(passwordResetToken);
    }

    @Override
    public String validatePasswordResetToken(String token) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(token);
        // checking if the token is present in the database oor not, if its present then its valid
        if (passwordResetToken == null) {
            // Invalid Token
            return "invalid";
        }
        User user = passwordResetToken.getUser();
        Calendar calendar = Calendar.getInstance();

        // Checking if that token is expired or not
        if ((passwordResetToken.getExpirationTime().getTime() - calendar.getTime().getTime()) <= 0) {
            // Now the token has expired, we should delete that token now
            passwordResetTokenRepository.delete(passwordResetToken);
            // Token is expired
            return "expired";
        }

        // Token is valid
        return "valid";
    }

    @Override
    public Optional<User> getUserByPasswordResetToken(String token) {
        return Optional.ofNullable(passwordResetTokenRepository.findByToken(token).getUser());
    }

    @Override
    public void changePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public boolean checkIfValidOldPassword(User user, String oldPassword) {
        // matches the input old password and the user's saved password
        return passwordEncoder.matches(oldPassword, user.getPassword());
    }
}
