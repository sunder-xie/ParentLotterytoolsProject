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
import com.BYL.lotteryTools.backstage.lotteryGroup.entity.LotteryGroup;
import com.BYL.lotteryTools.backstage.lotteryGroup.entity.RelaBindOfLbuyerorexpertAndGroup;
import com.BYL.lotteryTools.backstage.lotteryGroup.service.LotteryGroupService;
import com.BYL.lotteryTools.backstage.lotteryGroup.service.RelaBindbuyerAndGroupService;
import com.BYL.lotteryTools.backstage.lotterybuyerOfexpert.dto.LotterybuyerOrExpertDTO;
import com.BYL.lotteryTools.backstage.lotterybuyerOfexpert.entity.LotterybuyerOrExpert;
import com.BYL.lotteryTools.backstage.lotterybuyerOfexpert.service.LotterybuyerOrExpertService;
import com.BYL.lotteryTools.backstage.outer.repository.rongYunCloud.io.rong.models.CodeSuccessResult;
import com.BYL.lotteryTools.backstage.outer.service.RongyunImService;
import com.BYL.lotteryTools.common.bean.ResultBean;
import com.BYL.lotteryTools.common.entity.Uploadfile;
import com.BYL.lotteryTools.common.service.UploadfileService;
import com.BYL.lotteryTools.common.util.BeanUtil;
import com.BYL.lotteryTools.common.util.Constants;
import com.BYL.lotteryTools.common.util.QueryResult;

@Controller
@RequestMapping("/outerLGroup")
public class OuterLotteryGroupController
{
	private Logger logger = LoggerFactory.getLogger(OuterLotteryGroupController.class);
	
	@Autowired
	private RongyunImService rongyunImService;
	
	@Autowired
	private LotteryGroupService lotteryGroupService;
	
	@Autowired
	private LotterybuyerOrExpertService lotterybuyerOrExpertService;
	
	@Autowired
	private UploadfileService uploadfileService;
	
	@Autowired
	private RelaBindbuyerAndGroupService relaBindbuyerAndGroupService;
	
	public static final String SUCCESS_CODE = "200";//成功返回码
	
	/**
	 * 删除群
	* @Title: deleteGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param dto
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午1:51:25 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/deleteGroup", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> deleteGroup(
			LotteryGroupDTO dto,
			HttpServletRequest request,HttpSession httpSession)
	{
		Map<String,Object> map = new HashMap<String, Object>();
		
		//删除群的同时删除群绑定的机器人
		LotteryGroup entity = lotteryGroupService.getLotteryGroupById(dto.getId());
		entity.setGroupRobotID(null);
		
		//删除融云的群信息
		CodeSuccessResult result = rongyunImService.groupDismiss(dto.getOwnerId(), dto.getId());
		if(!OuterLotteryGroupController.SUCCESS_CODE.equals(result.getCode().toString()))
		{
			logger.error("融云删除群报错", result.getErrorMessage());
		}
		
		//删除数据库中的群信息
		entity.setIsDeleted(Constants.IS_DELETED);
		entity.setModify(dto.getOwnerId());
		entity.setModifyTime(new Timestamp(System.currentTimeMillis()));
		entity.setLotteryBuyerOrExpert(null);
		lotteryGroupService.update(entity);
		map.put("message", "删除成功");
		map.put("flag", true);
		
		return map;
	}
	
	/**
	 * 获取当前群的群成员
	* @Title: getMembersOfGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param groupId
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午4:44:23 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/getMembersOfGroup", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> getMembersOfGroup(
			String groupId,
			HttpServletRequest request,HttpSession httpSession)
	{
		Map<String,Object> map = new HashMap<String, Object>();
		
		LotteryGroup group = lotteryGroupService.getLotteryGroupById(groupId);
		//获取当前群和用户的关联关系(TODO:当前方法获取的群成员不包括群主和群内机器人)
		List<RelaBindOfLbuyerorexpertAndGroup> relalist = group.getRelaBindOfLbuyerorexpertAndGroups();
		
		List<LotterybuyerOrExpertDTO> userDtos = new ArrayList<LotterybuyerOrExpertDTO>();
		
		try
		{
			for (RelaBindOfLbuyerorexpertAndGroup rela : relalist)
			{
				LotterybuyerOrExpertDTO dto = new  LotterybuyerOrExpertDTO();
				BeanUtil.copyBeanProperties(dto, rela.getLotterybuyerOrExpert());
				userDtos.add(dto);
			}
			
			 map.put("flag", true);
			 map.put("message", "获取成功");
			 map.put("memberDtos", userDtos);
			
		}
		catch(Exception e)
		{
			logger.error("error:", e);
			 map.put("flag", false);
			 map.put("message", "获取失败");
		}
		
		
		return map;
	}
	
	/**
	 * 获取群列表
	* @Title: getGroupList 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param dto
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午5:06:49 
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
	 * 获取当前群主管理的群
	* @Title: getGroupsOfOwner 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param userId
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午3:52:17 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/getGroupsOfOwner", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> getGroupsOfOwner(
			String userId,
			HttpServletRequest request,HttpSession httpSession)
	{
		 Map<String,Object> map = new HashMap<String, Object>();
		 List<LotteryGroupDTO> groupDtos = new ArrayList<LotteryGroupDTO>();
		 
		 LotterybuyerOrExpert manager = lotterybuyerOrExpertService.getLotterybuyerOrExpertById(userId);
		 List<LotteryGroup> groups = manager.getLotteryGroups();
		 
		 try
		 {
			 for (LotteryGroup lotteryGroup : groups) 
			 {
				 LotteryGroupDTO dto = new LotteryGroupDTO();
				 BeanUtil.copyBeanProperties(dto, lotteryGroup);
				 groupDtos.add(dto);
			 }
			 
			 map.put("flag", true);
			 map.put("message", "获取成功");
			 map.put("groupDtos", groupDtos);
		 }
		 catch(Exception e)
		 {
			 logger.error("error:", e);
			 map.put("flag", false);
			 map.put("message", "获取失败");
		 }
		 
		 return map;
	}
	
	/**
	 * 获取当前用户加入的群
	* @Title: getGroupsOfUserjoins 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param UserId
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午3:00:47 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/getGroupsOfUserjoins", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> getGroupsOfUserjoins(
			String userId,
			HttpServletRequest request,HttpSession httpSession)
	{
		 Map<String,Object> map = new HashMap<String, Object>();
		 List<LotteryGroupDTO> groupDtos = new ArrayList<LotteryGroupDTO>();
		 LotterybuyerOrExpert user = lotterybuyerOrExpertService.getLotterybuyerOrExpertById(userId);
		 try
		 {
			 if(null != user)
			 {
				 List<RelaBindOfLbuyerorexpertAndGroup> relaGroups = user.getRelaBindOfLbuyerorexpertAndGroups();
				 
				 for (RelaBindOfLbuyerorexpertAndGroup relaGroup : relaGroups)
				 {
					if(null != relaGroup.getLotteryGroup())
					{
						LotteryGroupDTO dto = new LotteryGroupDTO();
						BeanUtil.copyBeanProperties(dto, relaGroup.getLotteryGroup());
						groupDtos.add(dto);
					}
				 }
				 
				 map.put("flag", true);
				 map.put("message", "获取成功");
				 map.put("groupDtos", groupDtos);
			 }
		 }
		 catch(Exception e)
		 {
			 logger.error("error:", e);
			 map.put("flag", false);
			 map.put("message", "获取失败");
		 }
		
		 
		 
		 return map;
	}
	
	/**
	 * 向群中加入用户
	* @Title: joinUserInGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param joinUsers
	* @param @param groupId
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午2:12:12 
	* @return ResultBean    返回类型 
	* @throws
	 */
	@RequestMapping(value="/joinUserInGroup", method = RequestMethod.GET)
	public @ResponseBody ResultBean joinUserInGroup(
			String[] joinUsers,String groupId,
			HttpServletRequest request,HttpSession httpSession)
	{
		ResultBean resultBean = new ResultBean();
		
		//建立群和要加入用户的关联
		LotteryGroup group = lotteryGroupService.getLotteryGroupById(groupId);
		
		LotterybuyerOrExpert user = null;
		for (String userId : joinUsers) 
		{
			user = lotterybuyerOrExpertService.getLotterybuyerOrExpertById(userId);
			RelaBindOfLbuyerorexpertAndGroup rela = new RelaBindOfLbuyerorexpertAndGroup();
			rela.setIsDeleted(Constants.IS_NOT_DELETED);
			rela.setIsReceive("1");
			rela.setIsTop("0");//是否置顶1：置顶 0：不置顶
			rela.setLotterybuyerOrExpert(user);
			rela.setLotteryGroup(group);
			rela.setCreator(groupId);
			rela.setCreateTime(new Timestamp(System.currentTimeMillis()));
			rela.setCreator(groupId);
			rela.setCreateTime(new Timestamp(System.currentTimeMillis()));

			//保存关联
			relaBindbuyerAndGroupService.save(rela);
		}
		
		//建立融云中群和用户的关系
		CodeSuccessResult result= rongyunImService.joinUserInGroup(joinUsers, groupId, group.getName());
		if(!OuterLotteryGroupController.SUCCESS_CODE.equals(result.getCode().toString()))
		{
			logger.error("融云群加入用户报错", result.getErrorMessage());
		}
		
		resultBean.setFlag(true);
		resultBean.setMessage("加入成功");
		
		return resultBean;
	}
	
	/**
	 * 从群中移除用户(机器人用户不可以被删除，前台也要做校验)
	* @Title: quitUserFronGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param quitUsers
	* @param @param groupId
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午2:12:02 
	* @return ResultBean    返回类型 
	* @throws
	 */
	@RequestMapping(value="/quitUserFronGroup", method = RequestMethod.GET)
	public @ResponseBody ResultBean quitUserFronGroup(
			String[] quitUsers,String groupId,
			HttpServletRequest request,HttpSession httpSession)
	{
		ResultBean resultBean = new ResultBean();
		
		//解除群和要加入用户的关联
		for (String userId : quitUsers) 
		{
			//根据用户id和群id获取关联关系
			RelaBindOfLbuyerorexpertAndGroup rela = relaBindbuyerAndGroupService.
					getRelaBindOfLbuyerorexpertAndGroupByUserIdAndGroupId(userId, groupId);
			LotterybuyerOrExpert user = lotterybuyerOrExpertService.getLotterybuyerOrExpertById(userId);
			//机器人用户不可以被删除
			if(null != rela && !"1".equals(user.getIsRobot()))
			{
				//删除关联
				rela.setLotterybuyerOrExpert(null);
				rela.setLotteryGroup(null);
				rela.setIsDeleted(Constants.IS_DELETED);
				relaBindbuyerAndGroupService.update(rela);
			}
			
		}
		
		//删除融云中群和用户的关系
		CodeSuccessResult result = rongyunImService.quitUserFronGroup(quitUsers, groupId);
		if(!OuterLotteryGroupController.SUCCESS_CODE.equals(result.getCode().toString()))
		{
			logger.error("融云执行用户退群时错误：", result.getErrorMessage());
		}
		
		resultBean.setFlag(true);
		resultBean.setMessage("退群成功");
		
		return resultBean;
	}
	
	/**
	 * 修改群信息
	* @Title: updateGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param dto
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 下午2:18:17 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/updateGroup", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> updateGroup(
			LotteryGroupDTO dto,
			HttpServletRequest request,HttpSession httpSession)
	{
		Map<String,Object> map = new HashMap<String, Object>();
		
		try
		{
			//修改群新新信息（修改群名称）
			if(null != dto.getName() && !"".equals(dto.getName()))
			{
				LotteryGroup group = lotteryGroupService.getLotteryGroupById(dto.getId());
				group.setName(dto.getName());
				//更改群名称
				lotteryGroupService.update(group);
				map.put("flag", true);
				map.put("message", "更新群信息成功");
				map.put("group", BeanUtil.copyBeanProperties(dto, group));
			}
			
			//刷新群的信息
			CodeSuccessResult result = rongyunImService.refreshGroup(dto.getId(), dto.getName());
			if(!OuterLotteryGroupController.SUCCESS_CODE.equals(result.getCode().toString()))
			{
				logger.error("融云刷新群信息时出错：", result.getErrorMessage());
			}
			
		}
		catch(Exception e)
		{
			logger.error("error:", e);
			map.put("flag", false);
			map.put("message", "更新群信息失败");
		}
		
		return map;
	}
	
	/**
	 * 创建群
	* @Title: createGroup 
	* @Description: TODO(这里用一句话描述这个方法的作用) 
	* @param @param dto
	* @param @param request
	* @param @param httpSession
	* @param @return    设定文件 
	* @author banna
	* @date 2017年4月24日 上午9:57:27 
	* @return Map<String,Object>    返回类型 
	* @throws
	 */
	@RequestMapping(value="/createGroup", method = RequestMethod.GET)
	public @ResponseBody Map<String,Object> createGroup(
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
			LotterybuyerOrExpert owner = lotterybuyerOrExpertService.
					getLotterybuyerOrExpertById(dto.getOwnerId());
			entity.setLotteryBuyerOrExpert(owner);//放置群与群主的关系
			
			//处理群头像
			if(null != dto.getTouXiangImg())
			{
				Uploadfile uploadfile = uploadfileService.uploadFiles(dto.getTouXiangImg(), request);
				StringBuffer imguri = new StringBuffer();//头像uri
				if(null != uploadfile)
				{//若头像不为空，则放置头像的uuid
					entity.setTouXiang(uploadfile.getNewsUuid());
					imguri.append(request.getContextPath()).
							append(uploadfile.getUploadfilepath()).
							append(uploadfile.getUploadRealName());
					logger.info("touxiang",imguri);//输出头像
				}
			}
			
			//TODO:创建群的同时创建群的机器人,如果区域彩种机器人已经存在，或者机器人加群数以及饱和，则要再创建机器人
			String robotUserId = lotterybuyerOrExpertService.
					createRobotUser(dto.getProvince(), dto.getCity(), dto.getLotteryType());
			
			entity.setGroupRobotID(robotUserId);
			
			//TODO:放置群等级
			entity.setGroupLevel("");
			entity.setIsDeleted(Constants.IS_NOT_DELETED);
			entity.setCreator(dto.getOwnerId());
			entity.setCreateTime(new Timestamp((System.currentTimeMillis())));
			entity.setModify(dto.getOwnerId());
			entity.setModifyTime(new Timestamp((System.currentTimeMillis())));
			//保存群信息
			lotteryGroupService.save(entity);
			
			//在融云创建群信息
			String[] joinUserId = {dto.getOwnerId(),robotUserId};//群主id加入要加入群的数组中,机器人加入群组中
			CodeSuccessResult result = rongyunImService.createGroup(joinUserId, entity.getId(), entity.getName());
			
			if(!SUCCESS_CODE.equals(result.getCode().toString()))
			{//若创建失败
				map.put("messsage", result.getErrorMessage());//创建失败返回融云端群创建失败信息
				logger.error("createGroup error:", result.getErrorMessage());
				map.put("flag", false);
			}
			else
			{
				map.put("message", "创建成功");
				map.put("flag", true);
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