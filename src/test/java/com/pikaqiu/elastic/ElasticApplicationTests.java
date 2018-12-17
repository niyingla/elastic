package com.pikaqiu.elastic;

import com.pikaqiu.elastic.entity.User;
import com.pikaqiu.elastic.repository.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sound.midi.Soundbank;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ElasticApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void contextLoads() {

        Optional<User> byId = userRepository.findById(1l);

        System.out.println(byId.get());

    }

}
