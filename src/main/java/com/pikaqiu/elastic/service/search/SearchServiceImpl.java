package com.pikaqiu.elastic.service.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Longs;
import com.pikaqiu.elastic.base.HouseSort;
import com.pikaqiu.elastic.base.RentValueBlock;
import com.pikaqiu.elastic.entity.House;
import com.pikaqiu.elastic.entity.HouseDetail;
import com.pikaqiu.elastic.entity.HouseTag;
import com.pikaqiu.elastic.repository.HouseDetailRepository;
import com.pikaqiu.elastic.repository.HouseRepository;
import com.pikaqiu.elastic.repository.HouseTagRepository;
import com.pikaqiu.elastic.repository.SupportAddressRepository;
import com.pikaqiu.elastic.service.ServiceMultiResult;
import com.pikaqiu.elastic.service.ServiceResult;
import com.pikaqiu.elastic.service.house.IAddressService;
import com.pikaqiu.elastic.web.form.MapSearch;
import com.pikaqiu.elastic.web.form.RentSearch;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by 瓦力.
 */
@Service
public class SearchServiceImpl implements ISearchService {
    private static final Logger logger = LoggerFactory.getLogger(ISearchService.class);

    private static final String INDEX_NAME = "xunwu";

    private static final String INDEX_TYPE = "house";

    private static final String INDEX_TOPIC = "house_build";

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Autowired
    private HouseTagRepository tagRepository;

    @Autowired
    private SupportAddressRepository supportAddressRepository;

    @Autowired
    private IAddressService addressService;

    private ModelMapper modelMapper = new ModelMapper();

    @Autowired
    private TransportClient esClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = INDEX_TOPIC)
    private void handleMessage(String content) {
        try {
            HouseIndexMessage houseIndexMessage = objectMapper.readValue(content, HouseIndexMessage.class);

            switch (houseIndexMessage.getOperation()) {
                case HouseIndexMessage.INDEX:
                    this.createOrUpdateIndex(houseIndexMessage);
                    break;
                case HouseIndexMessage.REMOVE:
                    this.removeIndex(houseIndexMessage);
                    break;
                default:
                    logger.warn("not support message content " + houseIndexMessage.getOperation());
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void index(Long houseId, int retry) {

        if (retry > HouseIndexMessage.MAX_RETRY) {
            logger.info("超过次数");
            return;
        } else {
            HouseIndexMessage houseIndexMessage = new HouseIndexMessage(houseId, HouseIndexMessage.INDEX, retry);

            try {
                kafkaTemplate.send(INDEX_TOPIC, objectMapper.writeValueAsString(houseIndexMessage));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void index(Long houseId) {
        //查库并封装数据到es对象
        House house = houseRepository.findById(houseId).get();

        if (house == null) {
            logger.error("house == null ");
            return;
        }

        HouseIndexTemplate houseIndexTemplate = new HouseIndexTemplate();

        modelMapper.map(house, houseIndexTemplate);

        HouseDetail houseDetail = houseDetailRepository.findByHouseId(houseId);

        modelMapper.map(houseDetail, houseIndexTemplate);

        List<HouseTag> houseTagList = tagRepository.findAllByHouseId(houseId);

        houseIndexTemplate.setTags(houseTagList.stream().map(HouseTag::getName).collect(Collectors.toList()));

        //创建预查询json
        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE).
                setQuery(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));

        logger.info(requestBuilder.toString());

        //查询
        SearchResponse searchResponse = requestBuilder.get();

        //获取命中数据 总条数
        long totalHits = searchResponse.getHits().getTotalHits();

        boolean success;

        if (totalHits == 0) {
            //没有就创建
            success = create(houseIndexTemplate);
        } else if (totalHits == 1) {
            //有一条修改
            String id = searchResponse.getHits().getAt(0).getId();

            success = update(id, houseIndexTemplate);
        } else {
            //有多条先删除 再创建
            success = deleteAndCreate(totalHits, houseIndexTemplate);
        }

        if (success) {
            logger.info("index success with house {}", houseId);
        }

    }

    private void createOrUpdateIndex(HouseIndexMessage message) {
        Long houseId = message.getHouseId();
        //查库并封装数据到es对象
        House house = houseRepository.findById(houseId).get();

        if (house == null) {
            logger.error("house == null ");
            return;
        }

        HouseIndexTemplate houseIndexTemplate = new HouseIndexTemplate();

        modelMapper.map(house, houseIndexTemplate);

        HouseDetail houseDetail = houseDetailRepository.findByHouseId(houseId);

        modelMapper.map(houseDetail, houseIndexTemplate);

        List<HouseTag> houseTagList = tagRepository.findAllByHouseId(houseId);

        houseIndexTemplate.setTags(houseTagList.stream().map(HouseTag::getName).collect(Collectors.toList()));

        //创建预查询json
        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE).
                setQuery(QueryBuilders.termQuery(HouseIndexKey.HOUSE_ID, houseId));

        logger.info(requestBuilder.toString());

        //查询
        SearchResponse searchResponse = requestBuilder.get();

        //获取命中数据 总条数
        long totalHits = searchResponse.getHits().getTotalHits();

        boolean success;

        if (totalHits == 0) {
            //没有就创建
            success = create(houseIndexTemplate);
        } else if (totalHits == 1) {
            //有一条修改
            String id = searchResponse.getHits().getAt(0).getId();

            success = update(id, houseIndexTemplate);
        } else {
            //有多条先删除 再创建
            success = deleteAndCreate(totalHits, houseIndexTemplate);
        }

        if (success) {
            logger.info("index success with house {}", houseId);
        }
    }


    private void removeIndex(HouseIndexMessage message) {
        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE.newRequestBuilder(esClient)
                .filter(QueryBuilders.termQuery("houseId", message.getHouseId())).source(INDEX_NAME);

        logger.info("delete by query for house" + builder);

        BulkByScrollResponse bulkByScrollResponse = builder.get();

        long deleted = bulkByScrollResponse.getDeleted();

        logger.info("delete total " + deleted);
    }


    private boolean create(HouseIndexTemplate houseIndexTemplate) {
        try {
            IndexResponse indexResponse = this.esClient.prepareIndex(INDEX_NAME, INDEX_TYPE)
                    //设置json转换成字节数组的类  告知类型为json
                    .setSource(objectMapper.writeValueAsBytes(houseIndexTemplate), XContentType.JSON).get();

            logger.info("Create index with house: " + houseIndexTemplate.getHouseId());

            if (indexResponse.status() == RestStatus.CREATED) {
                return true;
            } else {
                return false;
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("Create index with house error");
            return false;
        }
    }

    private boolean update(String houseIndexTemplateId, HouseIndexTemplate houseIndexTemplate) {
        try {
            UpdateResponse updateResponse = this.esClient.prepareUpdate(INDEX_NAME, INDEX_TYPE, houseIndexTemplateId)
                    //设置json转换成字节数组的类  告知类型为json
                    .setDoc(objectMapper.writeValueAsBytes(houseIndexTemplate), XContentType.JSON).get();

            logger.info("update with house: " + houseIndexTemplate.getHouseId());

            if (updateResponse.status() == RestStatus.OK) {
                return true;
            } else {
                return false;
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("update with house error");
            return false;
        }
    }

    /**
     * 先删除 再创建
     *
     * @param totalHit
     * @param indexTemplate
     * @return
     */
    private boolean deleteAndCreate(long totalHit, HouseIndexTemplate indexTemplate) {

        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE.newRequestBuilder(esClient)
                .filter(QueryBuilders.termQuery("houseId", indexTemplate.getHouseId())).source(INDEX_NAME);

        logger.info("delete by query for house" + builder);

        BulkByScrollResponse bulkByScrollResponse = builder.get();

        long deleted = bulkByScrollResponse.getDeleted();

        if (deleted != totalHit) {
            logger.warn("Need delete {} , but delete {} was deleted! ", totalHit, deleted);
            return false;
        }

        return create(indexTemplate);
    }

    public void remove(Long houseId, int retry) {
        if (retry > HouseIndexMessage.MAX_RETRY) {
            logger.info("超过次数");
            return;
        } else {
            HouseIndexMessage houseIndexMessage = new HouseIndexMessage(houseId, HouseIndexMessage.REMOVE, retry);

            try {
                kafkaTemplate.send(INDEX_TOPIC, objectMapper.writeValueAsString(houseIndexMessage));

            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public void remove(Long houseId) {

        DeleteByQueryRequestBuilder builder = DeleteByQueryAction.INSTANCE.newRequestBuilder(esClient)
                .filter(QueryBuilders.termQuery("houseId", houseId)).source(INDEX_NAME);

        logger.info("delete by query for house" + builder);

        BulkByScrollResponse bulkByScrollResponse = builder.get();

        long deleted = bulkByScrollResponse.getDeleted();

        logger.info("delete total " + deleted);

    }


    @Override
    public ServiceMultiResult<Long> query(RentSearch rentSearch) {

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //所在城市
        boolQueryBuilder.filter(QueryBuilders.termQuery(HouseIndexKey.CITY_EN_NAME, rentSearch.getCityEnName()));

        if (rentSearch.getRegionEnName() != null && !"*".equals(rentSearch.getRegionEnName())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(HouseIndexKey.REGION_EN_NAME, rentSearch.getRegionEnName()));
        }

        //关键词多字段匹配
        boolQueryBuilder.must(QueryBuilders.multiMatchQuery(rentSearch.getKeywords(), HouseIndexKey.TITLE,
                HouseIndexKey.TRAFFIC,
                HouseIndexKey.DISTRICT,
                HouseIndexKey.ROUND_SERVICE,
                HouseIndexKey.SUBWAY_LINE_NAME,
                HouseIndexKey.SUBWAY_STATION_NAME));


        RentValueBlock rentValueBlock = RentValueBlock.matchArea(rentSearch.getAreaBlock());

        if (!RentValueBlock.ALL.equals(rentValueBlock)) {
            //范围查询
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(HouseIndexKey.AREA);
            if (rentValueBlock.getMin() > 0) {
                //最小
                rangeQueryBuilder.gte(rentValueBlock.getMin());
            }
            if (rentValueBlock.getMax() > 0) {
                //最小
                rangeQueryBuilder.lte(rentValueBlock.getMax());
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }

        //具体朝向
        if (rentSearch.getDirection() > 0) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(HouseIndexKey.DIRECTION, rentSearch.getDirection()));
        }

        //租房方式 整租 | 合租
        if (rentSearch.getRentWay() > 0) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(HouseIndexKey.RENT_WAY, rentSearch.getRentWay()));
        }

        RentValueBlock matchPrice = RentValueBlock.matchPrice(rentSearch.getPriceBlock());

        if (!RentValueBlock.ALL.equals(matchPrice)) {
            //范围查询
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(HouseIndexKey.PRICE);

            if (matchPrice.getMin() > 0) {
                //最小
                rangeQueryBuilder.gte(matchPrice.getMin());
            }
            if (matchPrice.getMax() > 0) {
                //最小
                rangeQueryBuilder.lte(matchPrice.getMax());
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }

        SearchRequestBuilder requestBuilder = this.esClient.prepareSearch(INDEX_NAME).setTypes(INDEX_TYPE).setQuery(boolQueryBuilder).
                addSort(HouseSort.getSortKey(rentSearch.getOrderBy()), SortOrder.fromString(rentSearch.getOrderDirection()))
                .setFrom(rentSearch.getStart()).setSize(rentSearch.getSize());

        logger.info(requestBuilder.toString());

        List<Long> houseIds = new ArrayList<>();

        SearchResponse searchResponse = requestBuilder.get();

        if (searchResponse.status() != RestStatus.OK) {
            logger.error("查询失败");
            return new ServiceMultiResult<>(0, houseIds);
        }

        Iterator<SearchHit> iterator = searchResponse.getHits().iterator();

        while (iterator.hasNext()) {
            SearchHit searchHit = iterator.next();
            houseIds.add(Longs.tryParse(String.valueOf(searchHit.getSource().get(HouseIndexKey.HOUSE_ID))));

        }
        return new ServiceMultiResult<>(searchResponse.getHits().totalHits, houseIds);
    }

    @Override
    public ServiceResult<List<String>> suggest(String prefix) {
        return null;
    }

    @Override
    public ServiceResult<Long> aggregateDistrictHouse(String cityEnName, String regionEnName, String district) {
        return null;
    }

    @Override
    public ServiceMultiResult<HouseBucketDTO> mapAggregate(String cityEnName) {
        return null;
    }

    @Override
    public ServiceMultiResult<Long> mapQuery(String cityEnName, String orderBy, String orderDirection, int start, int size) {
        return null;
    }

    @Override
    public ServiceMultiResult<Long> mapQuery(MapSearch mapSearch) {
        return null;
    }


}
