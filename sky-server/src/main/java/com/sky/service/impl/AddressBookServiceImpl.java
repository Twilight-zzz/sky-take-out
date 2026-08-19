package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    AddressBookMapper addressBookMapper;
    /**
     * 新增地址
     * @param addressBook
     */
    public void save(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook) ;

    }

    /**
     * 根据传入的地址簿参数动态批量查询地址
     * @param addressBook
     * @return
     */
    public List<AddressBook> list(AddressBook addressBook) {
        return addressBookMapper.list(addressBook) ;
    }

    /**
     * 动态修改地址
     * @param addressBook
     */
    public void update(AddressBook addressBook) {
        addressBookMapper.update(addressBook);
    }

    /**
     * 根据id删除地址
     * @param id
     */
    public void deleteById(Long id){
        addressBookMapper.deleteById(id) ;
    }

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    public AddressBook getById(Long id){
        return addressBookMapper.getById(id) ;
    }

    /**
     * 设置默认地址
     * @param addressBook
     */
    @Transactional
    public void setDefault(AddressBook addressBook){
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.updateIsDefaultByUserId(addressBook) ;

        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook) ;

    };

}
