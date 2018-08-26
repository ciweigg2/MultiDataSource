package com.wbb.web.action;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.wbb.bean.DataSourceDO;
import com.wbb.dataSource.dynamic.DataSourceBean;
import com.wbb.dataSource.dynamic.DataSourceContext;
import com.wbb.dataSource.dynamic.DataSourceContextHolder;
import com.wbb.dataSource.dynamic.DynamicDataSource;
import com.wbb.mapper.DataSourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.wbb.bean.Cost;
import com.wbb.enums.EnumErrorCode;
import com.wbb.service.transaction.CostService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestAction {

	@Autowired
	private DataSourceMapper dataSourceMapper;

	@Resource
	private CostService costService;
	@RequestMapping(value="test1",method=RequestMethod.GET)
	public Object test1(){
		DataSourceContextHolder.setDataSource("d3");
		Cost cost = new Cost();
		cost.setMoney(100);
		costService.insert7(cost);
		return ResponseData.createResponseData(EnumErrorCode.OK, null);
	}

	@RequestMapping(value="test2",method=RequestMethod.GET)
	public Object test2(String key) throws Exception{
		Cost cost = new Cost();
		cost.setMoney(100);
		//设置指定数据源
		DataSourceContextHolder.setDataSource(key);
		costService.insert(cost,true);
		return ResponseData.createResponseData(EnumErrorCode.OK, costService.sum());
	}

	@RequestMapping(value="insert",method=RequestMethod.GET)
	public Object get() throws Exception{
//		DataSourceDO dataSourceDO = new DataSourceDO();
//		dataSourceDO.setDatabaseName(String.valueOf(System.currentTimeMillis()));
//		dataSourceDO.setUsername("root");
//		dataSourceDO.setDatasourceName("add" + System.currentTimeMillis());
//		dataSourceDO.setPassword("");
//		dataSourceDO.setDatabaseIp("127.0.0.1");
//		dataSourceDO.setDatabasePort("3306");
//		dataSourceMapper.insertSelective(dataSourceDO);
//
//		DataSourceBean dataSourceBean = new DataSourceBean(new DataSourceBean.DataSourceBeanBuilder(dataSourceDO.getDatasourceName(),
//				dataSourceDO.getDatabaseIp(), dataSourceDO.getDatabasePort(), dataSourceDO.getDatabaseName(),
//				dataSourceDO.getUsername(), dataSourceDO.getPassword()));
//
//		DataSourceContext.setDataSource(dataSourceBean);
		List<DataSourceDO> dataSourceDOList = dataSourceMapper.getAllDataSources();
		for (DataSourceDO dataSourceDO : dataSourceDOList) {
			DataSourceBean dataSourceBean = new DataSourceBean(new DataSourceBean.DataSourceBeanBuilder(dataSourceDO.getDatasourceName(),
					dataSourceDO.getDatabaseIp(), dataSourceDO.getDatabasePort(), dataSourceDO.getDatabaseName(),
					dataSourceDO.getUsername(), dataSourceDO.getPassword()));
			DataSourceContext.setDataSource(dataSourceBean);
			//必须执行查询才能设置数据源
			List<DataSourceDO> dataSourceDOList2 = dataSourceMapper.getAllDataSources();
			try {
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ResponseData.createResponseData(EnumErrorCode.OK, "重新设置成功");
	}

}
