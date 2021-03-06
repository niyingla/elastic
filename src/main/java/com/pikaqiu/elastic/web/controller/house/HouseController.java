package com.pikaqiu.elastic.web.controller.house;

import com.pikaqiu.elastic.base.ApiResponse;
import com.pikaqiu.elastic.base.RentValueBlock;
import com.pikaqiu.elastic.service.ServiceMultiResult;
import com.pikaqiu.elastic.service.ServiceResult;
import com.pikaqiu.elastic.service.house.IAddressService;
import com.pikaqiu.elastic.service.house.IHouseService;
import com.pikaqiu.elastic.service.search.ISearchService;
import com.pikaqiu.elastic.web.dto.HouseDTO;
import com.pikaqiu.elastic.web.dto.SubwayDTO;
import com.pikaqiu.elastic.web.dto.SubwayStationDTO;
import com.pikaqiu.elastic.web.dto.SupportAddressDTO;
import com.pikaqiu.elastic.web.form.RentSearch;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: elastic
 * @description:
 * @author: xiaoye
 * @create: 2018-12-09 15:35
 **/
@Controller
public class HouseController {

    @Autowired
    private IAddressService addressService;

    @Autowired
    private IHouseService houseService;

    @Autowired
    private ISearchService searchService;
    /**
     * 自动补全接口
     */
    @GetMapping("rent/house/autocomplete")
    @ResponseBody
    public ApiResponse autocomplete(@RequestParam(value = "prefix") String prefix) {

        if (prefix.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }
        ServiceResult<List<String>> result = this.searchService.suggest(prefix);
        return ApiResponse.ofSuccess(result.getResult());
    }

    /**
     * 获取支持城市列表
     *
     * @return
     */
    @GetMapping("address/support/cities")
    @ResponseBody
    public ApiResponse getSupportCities() {
        ServiceMultiResult<SupportAddressDTO> result = addressService.findAllCities();

        if (result.getResultSize() == 0) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(result.getResult());
    }

    /**
     * 获取对应城市支持区域列表
     *
     * @param cityEnName
     * @return
     */
    @GetMapping("address/support/regions")
    @ResponseBody
    public ApiResponse getSupportRegions(@RequestParam(name = "city_name") String cityEnName) {
        ServiceMultiResult<SupportAddressDTO> addressResult = addressService.findAllRegionsByCityName(cityEnName);
        if (addressResult.getResult() == null || addressResult.getTotal() < 1) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(addressResult.getResult());
    }

    /**
     * 获取具体城市所支持的地铁线路
     *
     * @param cityEnName
     * @return
     */
    @GetMapping("address/support/subway/line")
    @ResponseBody
    public ApiResponse getSupportSubwayLine(@RequestParam(name = "city_name") String cityEnName) {
        List<SubwayDTO> subways = addressService.findAllSubwayByCity(cityEnName);
        if (subways.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }

        return ApiResponse.ofSuccess(subways);
    }

    /**
     * 获取对应地铁线路所支持的地铁站点
     *
     * @param subwayId
     * @return
     */
    @GetMapping("address/support/subway/station")
    @ResponseBody
    public ApiResponse getSupportSubwayStation(@RequestParam(name = "subway_id") Long subwayId) {
        List<SubwayStationDTO> stationDTOS = addressService.findAllStationBySubway(subwayId);
        if (stationDTOS.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }

        return ApiResponse.ofSuccess(stationDTOS);
    }

    @GetMapping("rent/house")
    public String rentHousePage(@ModelAttribute RentSearch rentSearch, Model model, HttpSession session, RedirectAttributes redirectAttributes) {

        if (StringUtils.isBlank(rentSearch.getCityEnName())) {

            String cityEnName = (String) session.getAttribute("cityEnName");
            if (StringUtils.isBlank(cityEnName)) {
                redirectAttributes.addAttribute("msg", "must-chose-city");
            }else {
                rentSearch.setCityEnName(cityEnName);
            }
        }else {
            session.setAttribute("cityEnName", rentSearch.getCityEnName());

        }
        //设置当前城市
        ServiceResult<SupportAddressDTO> serviceCity = addressService.findCity(rentSearch.getCityEnName());

        if (!serviceCity.isSuccess()) {
            redirectAttributes.addAttribute("msg", "must_chose_city");
            return "redirect:/index";
        }
        model.addAttribute("currentCity", serviceCity.getResult());

        //
        ServiceMultiResult allRegionsByCityName = addressService.findAllRegionsByCityName(rentSearch.getCityEnName());

        if (allRegionsByCityName.getResult() == null || allRegionsByCityName.getTotal() < 1) {
            redirectAttributes.addAttribute("msg", "must-chose-city");
            return "redirect:/index";
        }

        ServiceMultiResult<HouseDTO> result = houseService.query(rentSearch);

        model.addAttribute("total", result.getTotal());

        model.addAttribute("houses", new ArrayList<>());

        if (rentSearch.getRegionEnName() == null) {
            rentSearch.setRegionEnName("*");
        }

        model.addAttribute("searchBody", rentSearch);
        model.addAttribute("regions", result.getResult());
        model.addAttribute("priceBlocks", RentValueBlock.PRICE_BLOCK);
        model.addAttribute("areaBlocks", RentValueBlock.AREA_BLOCK);

        model.addAttribute("currentPriceBlock", RentValueBlock.matchPrice(rentSearch.getPriceBlock()));
        model.addAttribute("currentAreaBlock", RentValueBlock.matchArea(rentSearch.getAreaBlock()));

        return "rent-list";
    }
}
