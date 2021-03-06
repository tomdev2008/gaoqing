/**
 * 0.0.0.1
 */
package com.ctvit.nba.service.impl;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.json.JSONObject;

import com.ctvit.nba.dao.ScheduleDao;
import com.ctvit.nba.dao.impl.ScheduleDaoImpl;
import com.ctvit.nba.entity.Schedule;
import com.ctvit.nba.expand.ScheduleUtil;
import com.ctvit.nba.service.ScheduleService;
import com.ctvit.nba.util.CommonUtil;
import com.ctvit.nba.util.JDBCUtil;
import com.ctvit.nba.util.URLContentUtil;
import com.ctvit.nba.util.URLUtil;
import com.ctvit.nba.util.XMLUtil;

/**
 * 赛程更新 Service 的实现类
 * @author 高青
 * 2013-11-28
 */
public class ScheduleServiceImpl implements ScheduleService {
	
	/** 日志对象 */
	private Logger log = Logger.getLogger(ScheduleServiceImpl.class);
	
	/** 判断执行次数的 Map 对象（外部 key 放唯一查询标识，内部 key 放唯一查询条件标识，在外部和内部形成唯一区别标识） */
	private static Map<String, Map<String, Integer>> countMap = new HashMap<String, Map<String,Integer>>();
	
	/**赛程 的  Dao 类*/
	private ScheduleDao scheduleDao = new ScheduleDaoImpl();
	
	/** 得到数据链接地址 */
	private Map<String, Map<String, String>> finalURLMap = null;

	@Override
	public <T> int updateSchedule(String moduleName, Map<String, Map<String, T>> uniqueRemarkerAndConditionMap,  Map<String, Schedule> tRemarkerAndParamsMap) {
		int flag = 0;
		
		/*
		 * 根据提供的地址，查询赛程数据
		 */
		List<Schedule> scheduleListByURL = getURLContent2ScheduleList(moduleName, uniqueRemarkerAndConditionMap, tRemarkerAndParamsMap);
		
		//链接地址标识
		String partURLRemarker = CommonUtil.getInnerUpdateModule(finalURLMap);
		
		//判断当前更新方式下，是否初始化过数据
		Map<String, Integer> map = countMap.get(partURLRemarker);
		
		/*
		 * 组织 查询条件标识 
		 */
		String selectConditionRemarker = CommonUtil.getConditionRemarker(uniqueRemarkerAndConditionMap);
		
		//更新到 XML 文件的更新标识(当前的 链接地址+查询条件条件标识，形成唯一的 XML 文件)
		String xmlUpdateRemarker = partURLRemarker + "-" + selectConditionRemarker;
		if (map == null) {
			/*
			 * 更新到数据库
			 */
			scheduleDao.updateSchedule2DB(scheduleListByURL);
			
			/*
			 * 更新指定  XML 文件
			 */
			List<Element> childrenElementList = ScheduleUtil.getChildrenElementList(scheduleListByURL);
			flag = XMLUtil.updateData2XML(moduleName, xmlUpdateRemarker, childrenElementList);
			
			Map<String, Integer> innerMap = new HashMap<String, Integer>();
			
			//将查询条件标识作为 key ，形成唯一的区分查询标识
			innerMap.put(selectConditionRemarker, 1);
			countMap.put(partURLRemarker, innerMap);
		}else {
			if (map.get(selectConditionRemarker) == null) {
				/*
				 * 更新到数据库
				 */
				scheduleDao.updateSchedule2DB(scheduleListByURL);
				
				/*
				 * 更新指定  XML 文件
				 */
				List<Element> childrenElementList = ScheduleUtil.getChildrenElementList(scheduleListByURL);
				flag = XMLUtil.updateData2XML(moduleName, xmlUpdateRemarker, childrenElementList);
				
				Map<String, Integer> innerMap = new HashMap<String, Integer>();
				innerMap.put(selectConditionRemarker, 1);
				countMap.put(selectConditionRemarker, innerMap);
			}else {
				log.info("当前更新方式为：" + partURLRemarker + ",更新的 key 为：" + selectConditionRemarker + ",已经初始化过数据，不在初始化了！");
			}
		}
		return flag;
	}
	
	/**
	 * 得到 URL 链接地址中的内容，并封装到 Player 实体集合中
	 * @author 高青
	 * 2014-1-23
	 * @param moduleName 模块名称
	 * @param innerUpdateModuleACondtions 内部更新模块（唯一链接标识）和更新条件 Map 对象
	 * @param updateModuleAlias 更新模块的别名
	 * @param tRemarkerAndParamsMap 实体类唯一标识和具体实体类封装的参数
	 * @return playerPersonalList 球员个人信息集
	 */
	private <T> List<Schedule> getURLContent2ScheduleList(String moduleName,
			Map<String, Map<String, T>> innerUpdateModuleACondtions,
			Map<String, Schedule> tRemarkerAndParamsMap) {
		//初始化球员信息数据集
		List<Schedule> scheduleList = null;
		
		//得到数据链接地址
		finalURLMap = URLUtil.getFinalURLMap(moduleName, innerUpdateModuleACondtions);
		
		//得到更新条件
		String keyConditions = CommonUtil.getKeyConditions(innerUpdateModuleACondtions);
		
		//得到内部链接模块名称（链接的唯一标识）
		String innerUpdateModule = CommonUtil.getInnerUpdateModule(finalURLMap);
		
		//得到最终的 URL 地址
		String url = URLUtil.getURL(finalURLMap);
		//得到“get”部分的链接地址
		String partURL = URLUtil.getPartGetURL(finalURLMap);
		
		//得到链接地址的数据集合
		JSONObject urlJsonObject = URLContentUtil.getURLJsonObject(url);
		
		String innerUpdateModule_otherInfo = innerUpdateModule + "," + keyConditions;
		
		//得到赛程实体类集
		scheduleList = ScheduleUtil.getScheduleList(innerUpdateModule_otherInfo, urlJsonObject, tRemarkerAndParamsMap);
		
		return scheduleList;
	}

	@Override
	public String getURLScheduleJSON(String moduleName, Map<String, Map<String, String>> uniqueRemarkerAndConditionMap) {
		String jsonSchedule = "";
		//得到相应的 url
		Map<String, Map<String, String>> finalURLMap = URLUtil.getFinalURLMap(moduleName, uniqueRemarkerAndConditionMap);
		//得到更新方式
		String innerUpdateModule = CommonUtil.getInnerUpdateModule(finalURLMap);
		//得到部分链接地址
		String partURL = URLUtil.getPartGetURL(finalURLMap);
		//得到 url 
		String completeURL = finalURLMap.get(innerUpdateModule).get(partURL);
		
		jsonSchedule = URLContentUtil.getURLContent(completeURL);
		return jsonSchedule;
	}

	@Override
	public <T> int updateSchedule2Outer(String moduleName, Map<String, Schedule> tRemarkerAndParamsMap, Map<String, Map<String, T>> uniqueRemarkerAndConditionMap) {
		int flag = 0;
		
		//得到唯一链接模块标识
		String uniqueMoeduleRemarker = CommonUtil.getMapKey(uniqueRemarkerAndConditionMap);
		
		//得到更新条件date
		String updateName = "";
		
		//每日更新赛程
		if(uniqueMoeduleRemarker != null && "SCHEDULES".equals(uniqueMoeduleRemarker)){
			updateName = (String) uniqueRemarkerAndConditionMap.get(uniqueMoeduleRemarker).get("date");
			
		//每月更新赛程
		}else if (uniqueMoeduleRemarker != null && "MONTH_SCHEDULE_LIST".equals(uniqueMoeduleRemarker)) {
			updateName = (String) uniqueRemarkerAndConditionMap.get(uniqueMoeduleRemarker).get("month");
		}
		
		//建立数据库连接
		Connection connection = JDBCUtil.getConnection();
		
		//更新到数据库中
		int maintainFlag = scheduleDao.maintainSchedule(connection, tRemarkerAndParamsMap);
		
		//得到 Schedule 集合对象
		List<Schedule> schedules = getSchedules(uniqueMoeduleRemarker, updateName);
		
		/*
		 * 将得到更改后的 Schedule集合对象更新到 xml 文件中
		 */
		List<Element> childrenElementList = ScheduleUtil.getChildrenElementList(schedules);
		
		//得到更新到 xml 文件的标识变量
		String xmlFolderRemarker = uniqueMoeduleRemarker + "-" + CommonUtil.getConditionRemarker(uniqueRemarkerAndConditionMap);
		flag = XMLUtil.updateData2XML(moduleName, xmlFolderRemarker, childrenElementList);
		
		return flag;
	}
	
	@Override
	public List<Schedule> getSchedules(String innerUpdateModule, String updateName) {
		//得到数据库更新后的数据对象集合
		List<Schedule> schedules = new ArrayList<Schedule>();
		
		//建立数据库连接
		Connection connection = JDBCUtil.getConnection();
		
		try {
			schedules = scheduleDao.getScheduleById(connection, innerUpdateModule, updateName);
		} catch (Exception e) {
			log.info("查询类别为：" + innerUpdateModule + "的，标识为：" + updateName + "的数据出现异常！");
			e.printStackTrace();
		}
		return schedules;
	}

}
