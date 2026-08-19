package com.sky.controller.user;


import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/user/addressBook")
@RestController("userAddressBookController")
@Slf4j
@Api( tags = "C端-地址簿相关接口")
public class AddressBookController {


    @Autowired
    private AddressBookService addressBookService;

    @PostMapping
    @ApiOperation("新增地址")
    public Result save(@RequestBody AddressBook addressBook){
        log.info("新增地址: {}" , addressBook) ;
        addressBookService.save(addressBook) ;
        return Result.success() ;
    }

    @GetMapping("/list")
    @ApiOperation("查询所有地址")
    public Result<List<AddressBook>> list(){
        AddressBook addressBook = new AddressBook() ;
        addressBook.setUserId(BaseContext.getCurrentId()) ;
        List<AddressBook> list = addressBookService.list(addressBook) ;
        return Result.success(list) ;
    }

    @GetMapping("/default")
    @ApiOperation("查询默认地址")
    public Result<AddressBook> getDefault(){
        AddressBook addressBook = new AddressBook() ;
        addressBook.setUserId(BaseContext.getCurrentId()) ;
        addressBook.setIsDefault(1);
        List<AddressBook> list = addressBookService.list(addressBook) ;
        if(list != null && !list.isEmpty()){
            return Result.success(list.get(0));
        }
        return Result.error("没有查询到默认地址");
    }

    @PutMapping
    @ApiOperation("/修改地址")
    public Result update(@RequestBody AddressBook addressBook){
        addressBookService.update(addressBook) ;
        return Result.success() ;
    }

    @DeleteMapping
    @ApiOperation("根据id删除地址")
    public Result delete(@RequestParam("id") Long id){
        addressBookService.deleteById(id) ;
        return Result.success() ;
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询地址")
    public Result<AddressBook> getById(@PathVariable("id") Long id){
        return Result.success(addressBookService.getById(id)) ;

    }


    @PutMapping("/default")
    @ApiOperation("设置默认地址")
    public Result setDefault(@RequestBody AddressBook addressBook){
        addressBookService.setDefault(addressBook) ;
        return Result.success() ;

    }
}
