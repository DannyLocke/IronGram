package com.theironyard.controllers;

import com.theironyard.entities.Photo;
import com.theironyard.entities.User;
import com.theironyard.services.PhotoRepository;
import com.theironyard.services.UserRepository;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.theironyard.utilities.PasswordStorage;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by dlocke on 1/3/17.
 */
@RestController
public class IronGramController {

    @Autowired
    UserRepository users;

    @Autowired
    PhotoRepository photos;

    Server dbui = null;

    //start
    @PostConstruct
    public void init() throws SQLException {
        dbui = Server.createWebServer().start();
    }

    

    //stop
    @PreDestroy
    public void destroy(){
        dbui.stop();
    }


    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public User login(String username, String password, HttpSession session,
                      HttpServletResponse response) throws Exception{

        User user = users.findFirstByName(username);

        //for new user
        if(user == null){
            user = new User(username, PasswordStorage.createHash(password));
            users.save(user);
        }
        //password check
        else if(!PasswordStorage.verifyPassword(password, user.getPassword())){
            throw new Exception("Wrong password");
        }

        session.setAttribute("username", username);
        response.sendRedirect("/");
        return user;
    }//end login()


    @RequestMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse response) throws IOException{
        session.invalidate();
        response.sendRedirect("/");
    }//end logout()


    @RequestMapping(path = "user", method = RequestMethod.GET)
    public User getUser(HttpSession session){
        String username = (String) session.getAttribute("username");
        return users.findFirstByName(username);
    }//end getUser()


    @RequestMapping("/upload")
    public Photo upload(HttpSession session, HttpServletResponse response, String receiver,
                        MultipartFile photo, Boolean isPublic, Long lifeTime) throws Exception{

        //check for username in the session
        String username = (String) session.getAttribute("username");
        if(username == null){
            throw new Exception("Not logged in.");
        }
 
        //use that username to tell us who the sender is
        User senderUser = users.findFirstByName(username);
        //use the receiver from the form to see who that user is
        User receiverUser = users.findFirstByName(receiver);

        //check for receiver name
        if(receiverUser == null) {
            throw new Exception("Receiver name doesn't exist.");
        }

        //check for correct file type
        if(!photo.getContentType().startsWith("image")){
            throw new Exception("Image files only please");
        }

        //create temporary file (turns photo into a file that can be saved)
        File photoFile = File.createTempFile("photo", photo.getOriginalFilename(), new File("public"));
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());

        //create actual photo object to be placed into db
        Photo p = new Photo();
        p.setSender(senderUser);
        p.setRecipient(receiverUser);
        p.setFilename(photoFile.getName());
        p.setPublic(isPublic);
        p.setLifeTime(lifeTime);
        photos.save(p); //save into photo repository

        response.sendRedirect("/");
        return p; //return photo object

    }//end public Photo upload()

    /*
    Create an input box in the upload form to let the user specify how many seconds they want the photo to exist.
    Store the number in the Photo entity. Then use that number instead of 10 seconds in your deletion code.

    Create a checkbox in the upload form to let the user specify whether the photo should be public.
    Store the boolean in the Photo entity.
     */

    @RequestMapping("/photos")
    public List<Photo> showPhotos(HttpSession session) throws Exception {

        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged in.");
        }

        User user = users.findFirstByName(username);

        List<Photo> allPhotos = (List<Photo>) photos.findAll();

        //check photo lifetime
        for(Photo photo: allPhotos) {
            if(photo.getLifeTime() == null) {
                photo.setLifeTime((long) 10);
            }
        }

        //post for selected time
        for (Photo photo : allPhotos){
            if(photo.getPostedTime() == null){
                photo.setPostedTime(LocalDateTime.now());
                photos.save(photo);
            }

            //delete after lifetime
            if(LocalDateTime.now().isAfter(photo.getPostedTime().plusSeconds(photo.getLifeTime()))){
                File deletePhoto = new File("public/"+photo.getFilename());
                deletePhoto.delete();
                photos.delete(photo);
            }
        }

        return photos.findByRecipient(user);

    }//end public List<Photo> showPhotos()

    /*  Delete photos from the database and the disk
    if they were viewed by the /photos route 10 or more seconds ago.
    This requirement is open-ended -- you need to figure out how to do it.
    */


    @RequestMapping(path = "/public-photos", method = RequestMethod.GET)
    public Iterable<Photo> jsonStrem(HttpSession session){
        String username = (String) session.getAttribute("userName");
        User sender = users.findFirstByName(username);
        return photos.findByIsPublic(sender);
    }

    /*
    Create a JSON route called /public-photos which takes a username as an argument.
    Make it return a list of photos sent by that user which were marked as public.
    (Remember: We're just returning JSON data here, not the actual photos.)
    */

}//end IronGramController
