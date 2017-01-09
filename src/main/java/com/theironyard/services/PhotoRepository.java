package com.theironyard.services;

import com.theironyard.entities.Photo;
import com.theironyard.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by dlocke on 1/3/17.
 */
public interface PhotoRepository extends CrudRepository<Photo, Integer>{

    //list of all photos for a given recipient
    List<Photo> findByRecipient(User receiver);

    //list of all public photos by sender
    List<Photo> findByIsPublic(User sender);
}
