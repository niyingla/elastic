package com.pikaqiu.elastic.service.house.impl;

import com.pikaqiu.elastic.base.HouseSubscribeStatus;
import com.pikaqiu.elastic.base.LoginUserUtil;
import com.pikaqiu.elastic.entity.*;
import com.pikaqiu.elastic.repository.*;
import com.pikaqiu.elastic.service.ServiceMultiResult;
import com.pikaqiu.elastic.service.ServiceResult;
import com.pikaqiu.elastic.service.house.IHouseService;
import com.pikaqiu.elastic.web.dto.HouseDTO;
import com.pikaqiu.elastic.web.dto.HouseDetailDTO;
import com.pikaqiu.elastic.web.dto.HousePictureDTO;
import com.pikaqiu.elastic.web.dto.HouseSubscribeDTO;
import com.pikaqiu.elastic.web.form.*;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @program: elastic
 * @description:
 * @author: xiaoye
 * @create: 2018-12-09 20:49
 **/
@Service
public class HouseServiceImpl implements IHouseService {

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Autowired
    private HousePictureRepository housePictureRepository;

    @Autowired
    private HouseTagRepository houseTagRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    @Autowired
    private SubwayRepository subwayRepository;

    @Value("${qiniu.cdn.prefix}")
    private String cdnPrefix;
    private ModelMapper mapper = new ModelMapper();

    @Override
    public ServiceResult<HouseDTO> save(HouseForm houseForm) {
        HouseDetail houseDetail = new HouseDetail();
        mapper.map(houseForm, houseDetail);
        ServiceResult<HouseDTO> subwayValidtionResult = wrapperDetailInfo(houseDetail, houseForm);
        if (subwayValidtionResult != null) {
            return subwayValidtionResult;
        }
        House house = new House();
        mapper.map(houseForm, house);
        Date now = new Date();
        house.setCreateTime(now);
        house.setCreateTime(now);
        house.setAdminId(LoginUserUtil.getUserId());
        houseRepository.save(house);
        houseDetail.setHouseId(house.getId());
        houseDetailRepository.save(houseDetail);
        //图片对象列表信息填充
        List<HousePicture> pictures = generatePictures(houseForm, house.getId());
        Iterable<HousePicture> housePictures = housePictureRepository.saveAll(pictures);

        //获取详情DTO
        HouseDTO houseDTO = mapper.map(house, HouseDTO.class);
        HouseDetailDTO houseDetailDTO = mapper.map(houseDetail, HouseDetailDTO.class);

        houseDTO.setHouseDetail(houseDetailDTO);

        //获取图片列表DTO
        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        housePictures.forEach(housePicture -> pictureDTOS.add(mapper.map(housePicture, HousePictureDTO.class)));
        houseDTO.setPictures(pictureDTOS);
        houseDTO.setCover(this.cdnPrefix + houseDTO.getCover());

        //获取所有标签
        List<String> tags = houseForm.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<HouseTag> houseTags = new ArrayList<>();
            for (String tag : tags) {
                houseTags.add(new HouseTag(house.getId(), tag));
            }
            //保存所有标签
            houseTagRepository.saveAll(houseTags);
            houseDTO.setTags(tags);
        }

        return new ServiceResult<>(true, null, houseDTO);

    }

    /**
     * 图片对象列表信息填充
     *
     * @param form
     * @param houseId
     * @return
     */
    private List<HousePicture> generatePictures(HouseForm form, Long houseId) {
        List<HousePicture> pictures = new ArrayList<>();
        //没有图片直接返回
        if (form.getPhotos() == null || form.getPhotos().isEmpty()) {
            return pictures;
        }
        //循环获取图片并设置值
        for (PhotoForm photoForm : form.getPhotos()) {
            HousePicture picture = new HousePicture();
            picture.setHouseId(houseId);
            picture.setCdnPrefix(cdnPrefix);
            picture.setPath(photoForm.getPath());
            picture.setWidth(photoForm.getWidth());
            picture.setHeight(photoForm.getHeight());
            pictures.add(picture);
        }
        return pictures;
    }

    /**
     * 房源详细信息对象填充
     *
     * @param houseDetail
     * @param houseForm
     * @return
     */
    private ServiceResult<HouseDTO> wrapperDetailInfo(HouseDetail houseDetail, HouseForm houseForm) {
        //查看地铁线路是否存在
        Subway subway = subwayRepository.findById(houseForm.getSubwayLineId()).get();
        if (subway == null) {
            return new ServiceResult<>(false, "Not valid subway line!");
        }
        //查看地铁站是否存在
        SubwayStation subwayStation = subwayStationRepository.findById(houseForm.getSubwayStationId()).get();
        if (subwayStation == null || subway.getId().equals(subwayStation.getSubwayId())) {
            return new ServiceResult<>(false, "Not valid subway station!");
        }

        houseDetail.setSubwayLineId(subway.getId());
        houseDetail.setSubwayLineName(subway.getName());

        houseDetail.setSubwayStationId(subwayStation.getId());
        houseDetail.setSubwayStationName(subwayStation.getName());

        houseDetail.setDescription(houseForm.getDescription());
        houseDetail.setDetailAddress(houseForm.getDetailAddress());
        houseDetail.setLayoutDesc(houseForm.getLayoutDesc());
        houseDetail.setRentWay(houseForm.getRentWay());
        houseDetail.setRoundService(houseForm.getRoundService());
        houseDetail.setTraffic(houseForm.getTraffic());
        return null;
    }


    /**
     * 修改
     * @param houseForm
     * @return
     */
    @Override
    @Transactional
    public ServiceResult update(HouseForm houseForm) {
        House house = houseRepository.findById(houseForm.getId()).get();
        if (house == null) {
            return ServiceResult.notFound();
        }

        //保存详情
        HouseDetail houseDetail = houseDetailRepository.findByHouseId(house.getId());

        if (houseDetail == null) {
            return ServiceResult.notFound();
        }

        //封装houseForm中的参数到houseDetail
        ServiceResult<HouseDTO> serviceResult = wrapperDetailInfo(houseDetail, houseForm);

        if (serviceResult != null) {
            return serviceResult;
        }

        houseDetailRepository.save(houseDetail);

        List<HousePicture> housePictures = generatePictures(houseForm, houseForm.getId());

        housePictureRepository.saveAll(housePictures);

        if (StringUtils.isBlank(houseForm.getCover())) {
            houseForm.setCover(house.getCover());
        }

        mapper.map(houseForm, house);

        houseRepository.save(house);

        return ServiceResult.success();
    }

    @Override
    public ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch searchBody) {
        List<HouseDTO> list = new ArrayList<>();

        //创建排序对象            设置DESC 设置排序字段
        Sort orders = new Sort(Sort.Direction.fromString(searchBody.getDirection()),"id");

        //获取页数  0 / 3 第0条开始 每页3条
        int page = searchBody.getStart() / searchBody.getLength();

        //创建分页对象
        Pageable pageable =  PageRequest.of(page, searchBody.getLength(), orders);

        Page<House> houses = houseRepository.findAll(pageable);

        //循环映射到DTO
        houses.forEach(house -> {
            HouseDTO houseDTO = mapper.map(house, HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix + house.getCover());
            list.add(houseDTO);
        });

        //所有条数和当前结果作为结果
        return new ServiceMultiResult(houses.getTotalElements(), list);
    }

    @Override
    public ServiceResult<HouseDTO> findCompleteOne(Long id) {
        Optional<House> house = houseRepository.findById(id);

        if (house.get() == null) {
            return ServiceResult.notFound();
        }
        HouseDetail houseDetail = houseDetailRepository.findByHouseId(id);

        HouseDetailDTO houseDetailDTO = mapper.map(houseDetail, HouseDetailDTO.class);

        List<HousePicture> housePictures = housePictureRepository.findAllByHouseId(id);

        List<HousePictureDTO> list = new ArrayList<>();

        housePictures.forEach(housePicture ->{

            HousePictureDTO picture = mapper.map(housePicture, HousePictureDTO.class);

            list.add(picture);
        });

        List<HouseTag> houseTags = houseTagRepository.findAllByHouseId(id);

        List<String> tags = houseTags.parallelStream().map(HouseTag::getName).collect(Collectors.toList());

        HouseDTO houseDTO = mapper.map(house.get(), HouseDTO.class);

        houseDTO.setHouseDetail(houseDetailDTO);

        houseDTO.setTags(tags);

        houseDTO.setPictures(list);

        return ServiceResult.of(houseDTO);
    }

    @Override
    public ServiceResult removePhoto(Long id) {
        return null;
    }

    @Override
    public ServiceResult updateCover(Long coverId, Long targetId) {
        return null;
    }

    @Override
    public ServiceResult addTag(Long houseId, String tag) {
        return null;
    }

    @Override
    public ServiceResult removeTag(Long houseId, String tag) {
        return null;
    }

    @Override
    public ServiceResult updateStatus(Long id, int status) {
        return null;
    }

    @Override
    public ServiceMultiResult<HouseDTO> query(RentSearch rentSearch) {

        


        return null;
    }

    @Override
    public ServiceMultiResult<HouseDTO> wholeMapQuery(MapSearch mapSearch) {
        return null;
    }

    @Override
    public ServiceMultiResult<HouseDTO> boundMapQuery(MapSearch mapSearch) {
        return null;
    }

    @Override
    public ServiceResult addSubscribeOrder(Long houseId) {
        return null;
    }

    @Override
    public ServiceMultiResult<Pair<HouseDTO, HouseSubscribeDTO>> querySubscribeList(HouseSubscribeStatus status, int start, int size) {
        return null;
    }

    @Override
    public ServiceResult subscribe(Long houseId, Date orderTime, String telephone, String desc) {
        return null;
    }

    @Override
    public ServiceResult cancelSubscribe(Long houseId) {
        return null;
    }

    @Override
    public ServiceMultiResult<Pair<HouseDTO, HouseSubscribeDTO>> findSubscribeList(int start, int size) {
        return null;
    }

    @Override
    public ServiceResult finishSubscribe(Long houseId) {
        return null;
    }
}
