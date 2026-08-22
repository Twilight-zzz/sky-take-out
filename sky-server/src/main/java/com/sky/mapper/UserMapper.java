package com.sky.mapper;


import com.sky.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("select * from User where openid = #{openId}")
    User getByOpenId(String openId);

    @Insert("""
insert into user (openid, name, phone, sex, id_number, avatar, create_time) 
values(#{openId} , #{name} , #{phone} , #{sex} , #{idNumber} , #{avatar} , #{createTime})
""")
    @Options(useGeneratedKeys = true , keyProperty = "id")
    void insert(User user) ;

    @Select("select * from user where id = #{id}")
    User getById(Long id) ;


    Integer countByMap( Map<String, Object> map) ;
}
