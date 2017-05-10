package com.BYL.lotteryTools.backstage.lotteryGroup.controller;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.BYL.lotteryTools.backstage.lotteryGroup.dto.LotteryGroupDTO;
import com.BYL.lotteryTools.backstage.lotteryGroup.entity.LGroupLevel;
import com.BYL.lotteryTools.backstage.lotteryGroup.entity.LotteryGroup;
import com.BYL.lotteryTools.backstage.lotteryGroup.entity.RelaGroupUpLevelRecord;
import com.BYL.lotteryTools.backstage.lotteryGroup.service.LGroupLevelService;
import com.BYL.lotteryTools.backstage.lotteryGroup.service.LotteryGroupService;
import com.BYL.lotteryTools.backstage.lotteryGroup.service.RelaGroupUpLevelService;
import com.BYL.lotteryTools.backstage.lotterybuyerOfexpert.entity.LotterybuyerOrExpert;
import com.BYL.lotteryTools.backstage.lotterybuyerOfexpert.service.LotterybuyerOrExpertService;
import com.BYL.lotteryTools.backstage.outer.repository.rongYunCloud.io.rong.models.CodeSuccessResult;
import com.BYL.lotteryTools.backstage.outer.service.RongyunImService;
import com.BYL.lotteryTools.common.entity.Uploadfile;
import com.BYL.lotteryTools.common.service.UploadfileService;
import com.BYL.lotteryTools.common.util.BeanUtil;
import com.BYL.lotteryTools.common.util.Constants;
import com.BYL.lotteryTools.common.util.QueryResult;

/**
 * 彩票群控制层
* @Description: TODO(这里用一句话描述这个方法的作用) 
* @author banna
* @date 2017年4月18日 下午4:52:17
 */
@Controller
@RequestMapping("/lgroup")
public class LotteryGroupController
{
	private Logger logger = LoggerFactory.getLogger(LotteryGroupController.class);
	
	@Autowired
	private LotteryGroupService lotteryGroupService;
	
	@Autowired
	private RelaGroupUpLevelService relaGroupUpLevelService;
	
	@Autowired
	private RongyunImService rongyunImService;
	
	@Autowired
	private LotterybuyerOrExpertService lotterybuyerOrExpertService;
	
	@Autowired
	private UploadfileService uploadfileService;
	
	@Autowired
	private LGroupLevelService lGroupLevelService;
	
	/**
	 * 获取群列表
	* @Title: getGroupList 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param dto
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年5月10日 下午1:39:05 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/getGroupList", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> getGroupList(
			LotteryGroupDTO dto,
			HttpServletRequest request,HttpSession httpSession)
	{
		Map<String,Object> map = new HashMap<String, Object>();
		
		//放置分页参数
		Pageable pageable = new PageRequest(0,Integer.MAX_VALUE);
		
		//参数
		StringBuffer buffer = new StringBuffer();
		List<Object> params = new ArrayList<Object>();
		
		//只查询未删除数据
		params.add("1");//只查询有效的数据
		buffer.append(" isDeleted = ?").append(params.size());
		
		
		if(null != dto.getProvince() && !"".equals(dto.getProvince())&& !Constants.PROVINCE_ALL.equals(dto.getProvince()))
		{
			params.add(dto.getProvince());
			buffer.append(" and province = ?").append(params.size());
		}
		
		if(null != dto.getCity() && !"".equals(dto.getCity())&& !Constants.CITY_ALL.equals(dto.getCity()))
		{
			params.add(dto.getCity());
			buffer.append(" and city = ?").append(params.size());
		}
		
		if(null != dto.getLotteryType() && !"".equals(dto.getLotteryType()))
		{
			params.add(dto.getLotteryType());
			buffer.append(" and lotteryType = ?").append(params.size());
		}
		
		if(null != dto.getId() && !"".equals(dto.getId()))
		{
			params.add(dto.getId());
			buffer.append(" and id = ?").append(params.size());
		}
		
		if(null != dto.getName() && !"".equals(dto.getName()))
		{
			params.add(dto.getName());
			buffer.append(" and name = ?").append(params.size());
		}
		
		
		//排序
		LinkedHashMap<String, String> orderBy = new LinkedHashMap<String, String>();
		orderBy.put("createTime", "desc");
		
		QueryResult<LotteryGroup> lQueryResult = lotteryGroupService
				.getLotteryGroupList(LotteryGroup.class,
				buffer.toString(), params.toArray(),orderBy, pageable);
				
		List<LotteryGroup> list = lQueryResult.getResultList();
		
		List<LotteryGroupDTO> dtos = lotteryGroupService.toDTOs(list);
		
		map.put("flag", true);
		map.put("message", "获取成功");
		map.put("groupDtos", dtos);
		
		return map;
	}
	
	/**
	 * 保存或修改群信息
	* @Title: saveOrUpdateGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param dto
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年5月10日 下午2:04:27 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/saveOrUpdateGroup")
	public @ResponseBody Map<String,Object> saveOrUpdateGroup(
			LotteryGroupDTO dto,
			HttpServletRequest request,HttpSession httpSession)
	{
		Map<String,Object> map = new HashMap<String, Object>();
		
		LotteryGroup entity = new LotteryGroup();
		//在服务器创建群信息
		try 
		{
			BeanUtil.copyBeanProperties(entity, dto);
			entity.setId(UUID.randomUUID().toString());//生成id
			
			entity.setGroupNumber(lotteryGroupService.generateGroupNumber());//放置群号
			LotterybuyerOrExpert owner = lotterybuyerOrExpertService.
					getLotterybuyerOrExpertById(dto.getOwnerId());
			entity.setLotteryBuyerOrExpert(owner);//放置群与群主的关系
			
			//处理群头像
			if(null != dto.getTouXiangImg())
			{
				Uploadfile uploadfile =null;
				String newsUuid = UUID.randomUUID().toString();
				try {
						 uploadfile = uploadfileService.uploadFiles(dto.getTouXiangImg(),request,newsUuid);
					
				} catch (Exception e) {
					logger.error("error:", e);
				}
				entity.setTouXiang(uploadfile.getNewsUuid());
			}
			
			//TODO:创建群的同时创建群的机器人,如果区域彩种机器人已经存在，或者机器人加群数以及饱和，则要再创建机器人
			String robotUserId = lotterybuyerOrExpertService.
					createRobotUser(dto.getProvince(), dto.getCity(), dto.getLotteryType());
			
			entity.setGroupRobotID(robotUserId);
			
			//TODO:放置群等级
			String level1Id = "1";//等级1群的等级id
			entity.setMemberCount(20);//以及群
			entity.setGroupLevel(level1Id);
			
			entity.setIsDeleted(Constants.IS_NOT_DELETED);
			entity.setCreator(dto.getOwnerId());
			entity.setCreateTime(new Timestamp((System.currentTimeMillis())));
			entity.setModify(dto.getOwnerId());
			entity.setModifyTime(new Timestamp((System.currentTimeMillis())));
			//保存群信息
			lotteryGroupService.save(entity);
			
			//放置群升级记录表数据
			RelaGroupUpLevelRecord level = new RelaGroupUpLevelRecord();
			LGroupLevel L1 = lGroupLevelService.getLGroupLevelByID(level1Id);//获取L1等级的实体数据
			level.setAfterLevel(L1);//一级群
			level.setBeforeLevel(null);
			level.setCreateTime(new Timestamp(System.currentTimeMillis()));
			level.setCreator(entity.getId());
			level.setIsDeleted(Constants.IS_NOT_DELETED);
			level.setLotteryGroup(entity);
			level.setModifyTime(new Timestamp(System.currentTimeMillis()));
			level.setModify(entity.getId());
			level.setOperator(dto.getOwnerId());
			relaGroupUpLevelService.save(level);//保存群等级记录表数据
			
			//在融云创建群信息
			String[] joinUserId = {dto.getOwnerId(),robotUserId};//群主id加入要加入群的数组中,机器人加入群组中
			CodeSuccessResult result = rongyunImService.createGroup(joinUserId, entity.getId(), entity.getName());
			
			if(!OuterLotteryGroupController.SUCCESS_CODE.equals(result.getCode().toString()))
			{//若创建失败
				map.put("messsage", result.getErrorMessage());//创建失败返回融云端群创建失败信息
				logger.error("createGroup error:", result.getErrorMessage());
				map.put("flag", false);
			}
			else
			{
				map.put("group", lotteryGroupService.toDTO(entity));//返回创建成功的群信息
				map.put("message", "创建成功");
				map.put("flag", true);
				
				//创建成功后，将当前群主的建群卡个数减1
			}
			
			
			
			
		} catch (Exception e) 
		{
			logger.error("error:", e);
			map.put("message", "创建失败");
			map.put("flag", false);
		}
		
		
		
		
		return map;
	}
	
	
}
