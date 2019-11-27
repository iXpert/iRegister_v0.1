package com.ixpert.iregister.v01.service;

import com.ixpert.iregister.v01.model.User;
import com.ixpert.iregister.v01.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("userService")
public class UserService{

    @Autowired
    private UserRepository userRepository;
    
    public User findByEmail(String email){
        return userRepository.findByEmail(email);
    }

    public User findByConfirmationToken(String confirmationToken){
        return userRepository.findByConfirmationToken(confirmationToken)
    }

    public void saveUser(User user){
        userRepository.save(user);
    }

}
