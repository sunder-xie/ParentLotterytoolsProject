package com.BYL.lotteryTools.backstage.lotteryManage.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.BYL.lotteryTools.backstage.lotteryManage.dto.LotteryPlayDTO;
import com.BYL.lotteryTools.backstage.lotteryManage.entity.LotteryPlay;
import com.BYL.lotteryTools.backstage.lotteryManage.repository.LotteryPlayPlanRepository;
import com.BYL.lotteryTools.backstage.lotteryManage.repository.LotteryPlayRepository;
import com.BYL.lotteryTools.backstage.lotteryManage.service.LotteryPlayService;
import com.BYL.lotteryTools.backstage.outer.entity.SrcfivedataDTO;
import com.BYL.lotteryTools.backstage.outer.repository.SrcfivedataDTORepository;
import com.BYL.lotteryTools.backstage.user.entity.Province;
import com.BYL.lotteryTools.backstage.user.service.ProvinceService;
import com.BYL.lotteryTools.common.util.BeanUtil;
import com.BYL.lotteryTools.common.util.DateUtil;
import com.BYL.lotteryTools.common.util.QueryResult;

@Service("lotteryPlayService")
@Transactional(propagation=Propagation.REQUIRED)
public class LotteryPlayServiceImpl implements LotteryPlayService 
{
	@Autowired
	private LotteryPlayRepository lotteryPlayRepository;//补录信息
	
	
	@Autowired
	private LotteryPlayPlanRepository lotteryPlayPlanRepository;//补录号码方案
	
	@Autowired
	private ProvinceService provinceService;
	
	@Autowired
	private SrcfivedataDTORepository srcfivedataDTORepository;
	


	public void save(LotteryPlay entity) {
		lotteryPlayRepository.save(entity);
	}


	public void update(LotteryPlay entity) {
		lotteryPlayRepository.save(entity);
		
	}


	public QueryResult<LotteryPlay> getLotteryPlayList(
			Class<LotteryPlay> entityClass, String whereJpql,
			Object[] queryParams, LinkedHashMap<String, String> orderby,
			Pageable pageable) {
		QueryResult<LotteryPlay> qResult = lotteryPlayRepository.getScrollDataByJpql(entityClass, whereJpql, queryParams,
				orderby, pageable);
		
		return qResult;
	}
	
	public List<LotteryPlay> getAllLotteryPlays()
	{
		return lotteryPlayRepository.getLotteryPlayList();
	}
	
	
	public QueryResult<LotteryPlay> getProvinceOfLotteryPlayList(Class<LotteryPlay> entityClass, String whereJpql, Object[] queryParams, 
			LinkedHashMap<String, String> orderby, Pageable pageable)
	{
		
		StringBuffer sql = new StringBuffer("SELECT u.* FROM T_BYL_LOTTERYPLAY u WHERE u.IS_DELETED='1'  GROUP BY u.PROVINCE");
		QueryResult<LotteryPlay> userObj = lotteryPlayRepository.
			getScrollDataBySql(LotteryPlay.class,sql.toString(), queryParams, pageable);
		return userObj;
	}
	
	public List<LotteryPlay> getLotteryPlayByProvinceAndLotteryType(String city,
			String lotteryType) {
		return lotteryPlayRepository.getLotteryPlayByProvinceAndLotteryType(city, lotteryType);
	}


	public List<LotteryPlayDTO> toRDTOS(List<LotteryPlay> entities) {
		List<LotteryPlayDTO> dtos = new ArrayList<LotteryPlayDTO>();
		LotteryPlayDTO dto;
		for (LotteryPlay entity : entities) 
		{
			dto = toDTO(entity);
			dtos.add(dto);
		}
		return dtos;
	}


	public LotteryPlayDTO toDTO(LotteryPlay entity) {
		LotteryPlayDTO dto = new LotteryPlayDTO();
		try {
			BeanUtil.copyBeanProperties(dto, entity);
			
			//处理实体中的特殊转换值
			if(null != entity.getCreateTime())//创建时间
			{
				dto.setCreateTime(DateUtil.formatDate(entity.getCreateTime(), DateUtil.FULL_DATE_FORMAT));
			}
			
			if(null != entity.getLotteryPlayBulufangan())//放置补录方案id
			{
				dto.setLtblPlaneId(entity.getLotteryPlayBulufangan().getId());
			}
			
			if(null != entity.getProvince())
			{
				Province province = new Province();
				province = provinceService.getProvinceByPcode(entity.getProvince());
				dto.setProvinceName(null != province?province.getPname():"");
			}
			
			
			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dto;
	}


	public LotteryPlay getLotteryPlayById(String id) {
		return lotteryPlayRepository.getLotteryPlayById(id);
	}


	public List<LotteryPlay> getLotteryPlayByProvince(String province) {
		return lotteryPlayRepository.getLotteryPlayByProvince(province);
	}

	public String getYuceMaxIssueId(String lotteryPlayId)
	{
		String issueId = "";
		LotteryPlay lotteryPlay = this.getLotteryPlayById(lotteryPlayId);
		String tbName = lotteryPlay.getCorrespondingTable();
		String lineCount = lotteryPlay.getLineCount();//获取每天开出的最大期数

		String execSql = "SELECT u.* FROM "+tbName +" u  order by ISSUE_NUMBER desc LIMIT 1 ";
		Object[] queryParams = new Object[]{
		};
		SrcfivedataDTO fiveDto = srcfivedataDTORepository.getEntityBySql(SrcfivedataDTO.class,execSql, queryParams);
		
		if(null != fiveDto)
		{
			issueId = getNextIssueByCurrentIssue(fiveDto.getIssueNumber(), lineCount);
		}
		
		return issueId;
				
	}

	private static String getNextIssueByCurrentIssue(String issueNumber,String lineCount)
	  {
	    String issueCode = issueNumber.substring(issueNumber.length() - 2, issueNumber.length());
	    int issue = Integer.parseInt(issueCode);
	    int nextIssue = (issue + 1) % Integer.parseInt(lineCount);
	    if (nextIssue > 9) 
	    {
	      return issueNumber.substring(0, issueNumber.length() - 2) + nextIssue;
	    }
	    if (nextIssue == 0)
	    {
	      return issueNumber.substring(0, issueNumber.length() - 2) + lineCount;
	    }
	    if (nextIssue == 1) 
	    {
	      return DateUtil.getNextDayStr(issueNumber.substring(0, issueNumber.length() - 2)) + "01";
	    }
	    return issueNumber.substring(0, issueNumber.length() - 2) + "0" + nextIssue;
	  }
	
	
	
	
	
}
