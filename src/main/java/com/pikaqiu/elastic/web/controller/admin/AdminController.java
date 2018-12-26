package com.pikaqiu.elastic.web.controller.admin;

import com.pikaqiu.elastic.base.ApiDataTableResponse;
import com.pikaqiu.elastic.base.ApiResponse;
import com.pikaqiu.elastic.entity.SupportAddress;
import com.pikaqiu.elastic.service.IUserService;
import com.pikaqiu.elastic.service.ServiceMultiResult;
import com.pikaqiu.elastic.service.ServiceResult;
import com.pikaqiu.elastic.service.house.IAddressService;
import com.pikaqiu.elastic.service.house.IHouseService;
import com.pikaqiu.elastic.service.house.impl.AddressServiceImpl;
import com.pikaqiu.elastic.service.search.ISearchService;
import com.pikaqiu.elastic.web.dto.*;
import com.pikaqiu.elastic.web.form.DatatableSearch;
import com.pikaqiu.elastic.web.form.HouseForm;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.elasticsearch.search.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @program: elastic
 * @description:
 * @author: xiaoye
 * @create: 2018-11-26 22:21
 **/
@Controller
public class AdminController {

    @Autowired
    private IAddressService addressService;

    @Autowired
    private IHouseService houseService;
    @Autowired
    private IUserService userService;

    /**
     * 后台管理中心
     *
     * @return
     */
    @GetMapping("/admin/center")
    public String adminCenterPage() {
        return "admin/center";
    }

    /**
     * 欢迎页
     *
     * @return
     */
    @GetMapping("/admin/welcome")
    public String welcomePage() {
        return "admin/welcome";
    }

    /**
     * 管理员登录页
     *
     * @return
     */
    @GetMapping("/admin/login")
    public String adminLoginPage() {
        return "admin/login";
    }

    @RequestMapping("/")
    public String index(Model model, HttpServletResponse response) {
        return "/index";
    }

    /**
     * 房源列表页
     *
     * @return
     */
    @GetMapping("admin/house/list")
    public String houseListPage() {
        return "admin/house-list";
    }

    @GetMapping("/admin/add/house")
    public String houseAdd() {
        return "admin/house-add";
    }

    @PostMapping(value = "admin/upload/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ApiResponse uploadPhoto(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }

        String fileName = file.getOriginalFilename();

        try {
            InputStream inputStream = file.getInputStream();
           /* Response response = qiNiuService.uploadFile(inputStream);
            if (response.isOK()) {
                QiNiuPutRet ret = gson.fromJson(response.bodyString(), QiNiuPutRet.class);
                return ApiResponse.ofSuccess(ret);
            } else {
                return ApiResponse.ofMessage(response.statusCode, response.getInfo());
            }*/

        } catch (QiniuException e) {
            Response response = e.response;
            try {
                return ApiResponse.ofMessage(response.statusCode, response.bodyString());
            } catch (QiniuException e1) {
                e1.printStackTrace();
                return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
        }
        return null;
    }

    /**
     * 后台获取房子列表
     * @param datatableSearch
     * @return
     */
    @PostMapping("admin/houses")
    @ResponseBody
    public ApiDataTableResponse houses(@ModelAttribute DatatableSearch datatableSearch){
        ServiceMultiResult<HouseDTO> houseDTOServiceMultiResult = houseService.adminQuery(datatableSearch);
        ApiDataTableResponse apiDataTableResponse = new ApiDataTableResponse(ApiResponse.Status.SUCCESS);
        //设置查询数据
        apiDataTableResponse.setData(houseDTOServiceMultiResult.getResult());
        apiDataTableResponse.setRecordsFiltered(houseDTOServiceMultiResult.getTotal());
        apiDataTableResponse.setRecordsTotal(houseDTOServiceMultiResult.getTotal());
        apiDataTableResponse.setDraw(datatableSearch.getDraw());
        //返回结果
        return apiDataTableResponse;
    }

    @PostMapping("admin/add/house")
    @ResponseBody
    public ApiResponse addHouse(@Valid @ModelAttribute("form-house-add") HouseForm houseForm, BindingResult bindingResult) {
        //数据校验
        if (bindingResult.hasErrors()) {
            return new ApiResponse(HttpStatus.BAD_REQUEST.value(), bindingResult.getAllErrors().get(0).getDefaultMessage(), null);
        }
        //判断图片
        if (CollectionUtils.isEmpty(houseForm.getPhotos()) || houseForm.getCover() == null) {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), "必须上传图片");
        }
        //判断地址
        Map<SupportAddress.Level, SupportAddressDTO> cityAndRegion = addressService.findCityAndRegion(houseForm.getCityEnName(), houseForm.getRegionEnName());
        if (cityAndRegion.size() != 2) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }

        //保存数据
        ServiceResult<HouseDTO> result = houseService.save(houseForm);

        if(result.isSuccess()){
            return ApiResponse.ofSuccess(result.getMessage());
        }

        return  ApiResponse.ofSuccess(ApiResponse.Status.NOT_VALID_PARAM);
    }
    /**
     * 房源信息编辑页
     * @return
     */
    @GetMapping("admin/house/edit")
    public String houseEditPage(@RequestParam(value = "id") Long id, Model model) {

        if (id == null || id < 1) {
            return "404";
        }

        ServiceResult<HouseDTO> serviceResult = houseService.findCompleteOne(id);
        if (!serviceResult.isSuccess()) {
            return "404";
        }
        HouseDTO result = serviceResult.getResult();
        model.addAttribute("house",result);

        //获取地址
        Map<SupportAddress.Level, SupportAddressDTO> cityAndRegion = addressService.findCityAndRegion(result.getCityEnName(), result.getRegionEnName());

        model.addAttribute("city", cityAndRegion.get(SupportAddress.Level.CITY));
        //获取地铁站
        model.addAttribute("region", cityAndRegion.get(SupportAddress.Level.REGION));

        ServiceResult<SubwayDTO> subway = addressService.findSubway(result.getHouseDetail().getSubwayLineId());

        if(subway.isSuccess()){
            model.addAttribute("subway", subway.getResult());
        }

        ServiceResult<SubwayStationDTO> subwayStation = addressService.findSubwayStation(result.getHouseDetail().getSubwayStationId());

        if(subway.isSuccess()){
            model.addAttribute("station", subwayStation.getResult());
        }
        return "admin/house-edit";
    }
    @PostMapping("admin/house/edit")
    @ResponseBody
    public ApiResponse editHouse(@Valid @ModelAttribute("form-house-edit") HouseForm houseForm, BindingResult bindingResult) {
        //数据校验
        if (bindingResult.hasErrors()) {
            return new ApiResponse(HttpStatus.BAD_REQUEST.value(), bindingResult.getAllErrors().get(0).getDefaultMessage(), null);
        }
        //判断图片
        if (CollectionUtils.isEmpty(houseForm.getPhotos()) || houseForm.getCover() == null) {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), "必须上传图片");
        }
        //判断地址
        Map<SupportAddress.Level, SupportAddressDTO> cityAndRegion = addressService.findCityAndRegion(houseForm.getCityEnName(), houseForm.getRegionEnName());
        if (cityAndRegion.size() != 2) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }

        //保存数据
        ServiceResult<HouseDTO> result = houseService.update(houseForm);

        if(result.isSuccess()){
            return ApiResponse.ofSuccess(result.getMessage());
        }

        return  ApiResponse.ofSuccess(ApiResponse.Status.NOT_VALID_PARAM);
    }

    @GetMapping("testAsync")
    @ResponseBody
    public String testAsync(){
        Future<String> stringFuture = userService.testAsync();
        try {
            System.out.println(stringFuture.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return "1111";
    }
}