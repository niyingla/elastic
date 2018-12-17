package com.pikaqiu.elastic.repository;

import com.pikaqiu.elastic.entity.SupportAddress;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * @program: elastic
 * @description:
 * @author: xiaoye
 * @create: 2018-12-09 15:36
 **/

public interface SupportAddressRepository extends CrudRepository<SupportAddress, Long> {

    List<SupportAddress> findAllByLevel(String level);

    SupportAddress findByEnNameAndLevel(String enName, String level);

    SupportAddress findByEnNameAndBelongTo(String enName, String belongTo);

    List<SupportAddress> findAllByLevelAndBelongTo(String level, String belongTo);


}
