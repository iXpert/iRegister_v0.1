package com.ixpert.iregister.v01.controller;

import com.ixpert.iregister.v01.model.User;
import com.ixpert.iregister.v01.service.EmailService;
import com.ixpert.iregister.v01.service.UserService;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;
import java.util.UUID;

@Controller
public class RegisterController {

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private UserService userService;
    @Autowired
    private EmailService emailService;

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public ModelAndView showRegisrationPage(ModelAndView modelAndView, User user){
        modelAndView.addObject("user", user);
        modelAndView.setViewName("register");
        return modelAndView
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ModelAndView processRegistrationForm(ModelAndView modelAndView, @Valid User user, BindingResult bindingResult, HttpServletRequest request){

        User userExists = userService.findByEmail(user.getEmail());
        System.out.println(userExists);

        if (userExists != null){
            modelAndView.addObject("alreadyRegisteredMessage","There is already a user registered with the email provided.");
            modelAndView.setViewName("register");
            bindingResult.reject("email");
        }
        if (bindingResult.hasErrors()){
            modelAndView.setViewName("register");
        } else { // new user --> create user and send confirmation email

            // disable user until email is confirmed
            user.setEnabled(false);

            // generate random character as a string token
            user.setConfirmationToken(UUID.randomUUID().toString());

            userService.saveUser(user);

            String appUrl = request.getScheme()+"://"+request.getServerName();

            SimpleMailMessage registrationEmail = new SimpleMailMessage();
            registrationEmail.setTo(user.getEmail());
            registrationEmail.setSubject("Registration Email");
            registrationEmail.setText("To confirm your email address, please click the link below:\n"+appUrl+"/confirm?token="+user.getConfirmationToken());
            registrationEmail.setFrom("info@ixpert.org");

            emailService.sendEmail(registrationEmail);

            modelAndView.addObject("confirmationMessage","A confirmation email has been sent to "+user.getEmail());
            modelAndView.setViewName("register");
        }

        return modelAndView;

    }

    // process confirmation link
    @RequestMapping(value = "/confirm", method = RequestMethod.GET)
    public ModelAndView confirmRegistration(ModelAndView modelAndView, @RequestParam("token") String token){
        User user = userService.findByConfirmationToken(token);

        if (user == null){ // no token found in DB
            modelAndView.addObject("invalidToken","This is invalid confirmation link");
        } else  { // token found
            modelAndView.addObject("confirmationToken",user.getConfirmationToken());
        }

        modelAndView.setViewName("confirm");
        return modelAndView;
    }

    // process confirmation link
    @RequestMapping(value = "/confirm",method = RequestMethod.POST)
    public ModelAndView confirmRegistration(ModelAndView modelAndView, BindingResult bindingResult,
                                            @RequestParam Map<String, String> requestParams, RedirectAttributes redir){

        modelAndView.setViewName("confirm");

        Zxcvbn passwordCheck = new Zxcvbn();

        Strength strength = passwordCheck.measure(requestParams.get("password"));

        if (strength.getScore() < 3){
            bindingResult.reject("password");
            redir.addFlashAttribute("errorMessage","Your password is too weak. Choose a stronger one.");
            modelAndView.setViewName("redirect:confirm?token="+requestParams.get("token"));
            System.out.println(requestParams.get("token"));
            return modelAndView;
        }
        // find the user associated with the reset token
        User user = userService.findByConfirmationToken(requestParams.get("token"));

        //set new password
        user.setPassword(bCryptPasswordEncoder.encode(requestParams.get("password")));

        //set user to enabled
        user.setEnabled(true);

        //save user
        userService.saveUser(user);

        modelAndView.addObject("successMessage","Your password has been set");
        return modelAndView;


    }



}
