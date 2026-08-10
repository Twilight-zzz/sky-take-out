package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     * @param employeeLoginDTO 员工登录信息
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // TODO 后期需要进行md5加密，然后再进行比对
        password = DigestUtils.md5DigestAsHex(password.getBytes()) ;
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }


    /**
     * 新增员工
     * @param employeeDTO 员工信息
     */
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee() ;

        //对象属性的拷贝
        BeanUtils.copyProperties(employeeDTO , employee) ;

        //补全剩下的
        //密码默认为123456，员工后续可自己修改
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()) ) ;
        //员工状态默认为1：启用
        employee.setStatus(StatusConstant.ENABLE) ;
        //创建时间修改时间创建用户修改用户
//        employee.setCreateTime(LocalDateTime.now()) ;
//        employee.setUpdateTime(LocalDateTime.now()) ;
//        employee.setCreateUser(BaseContext.getCurrentId()) ;
//        employee.setUpdateUser(BaseContext.getCurrentId()) ;

        //最后调用mapper操作数据库
        employeeMapper.insert(employee) ;

    }

    /**
     * 员工分页查询
     */
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        PageHelper.startPage(employeePageQueryDTO.getPage() , employeePageQueryDTO.getPageSize()) ;
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO) ;
        long total = page.getTotal() ;
        List<Employee> records = page.getResult() ;
        return new PageResult(total , records) ;
    }


    /**
     * 启用或禁用员工账号
     * @param status 员工状态
     * @param id 员工id
     */
    public void startOrStop(Integer status , Long id){
        Employee employee = Employee.builder().id(id).status(status).build() ;
        employeeMapper.update(employee) ;
    }

    /**
     * 根据id查询员工信息
     * @param id 员工id
     * @return employee
     */
    public Employee getById(Long id){
        Employee employee = employeeMapper.getById(id);
        employee.setPassword("****") ;
        return employee ;
    }

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    public void update(EmployeeDTO employeeDTO){
        Employee employee = new Employee() ;
        BeanUtils.copyProperties(employeeDTO , employee) ;
//        employee.setUpdateTime(LocalDateTime.now()) ;
//        employee.setUpdateUser(BaseContext.getCurrentId()) ;
        employeeMapper.update(employee) ;
    }


}
