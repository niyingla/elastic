package com.pikaqiu.elastic;

import com.pikaqiu.elastic.entity.User;
import com.pikaqiu.elastic.repository.UserRepository;
import com.pikaqiu.elastic.service.search.ISearchService;
import org.elasticsearch.search.SearchService;
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

    @Autowired
    private ISearchService searchService;

    @Test
    public void contextLoads() {
//        searchService.index(19L);


//        searchService.remove(19L);
    }

}
