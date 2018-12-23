package com.pikaqiu.elastic;

import com.pikaqiu.elastic.entity.User;
import com.pikaqiu.elastic.repository.UserRepository;
import com.pikaqiu.elastic.service.ServiceMultiResult;
import com.pikaqiu.elastic.service.search.HouseIndexKey;
import com.pikaqiu.elastic.service.search.ISearchService;
import com.pikaqiu.elastic.service.search.SearchServiceImpl;
import com.pikaqiu.elastic.web.form.RentSearch;
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
    private SearchServiceImpl searchService;


    @Test
    public void contextLoads() {
       /* for (long i =15L; i < 27l; i++) {
            searchService.index(i,1);
        }*/

        testSearch();

//        searchService.remove(19L);


    }

    private void testSearch(){
        RentSearch rentSearch = new RentSearch();
        rentSearch.setCityEnName("bj");
        rentSearch.setRegionEnName("*");
        rentSearch.setStart(0);
        rentSearch.setSize(10);
        rentSearch.setKeywords("时尚");
        rentSearch.setPriceBlock("1000-3000");
        rentSearch.setOrderBy(HouseIndexKey.CREATE_TIME);
        searchService.query(rentSearch);
        ServiceMultiResult<Long> query = searchService.query(rentSearch);
        System.out.println(query.getResult());
    }
}
