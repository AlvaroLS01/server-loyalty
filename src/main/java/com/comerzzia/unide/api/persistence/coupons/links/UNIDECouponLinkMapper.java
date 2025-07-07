package com.comerzzia.unide.api.persistence.coupons.links;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

@Mapper
public interface UNIDECouponLinkMapper {
	
    long countByExample(UNIDECouponLinkExample example);

    int deleteByExample(UNIDECouponLinkExample example);

    int insert(UNIDECouponLink record);

    int insertSelective(UNIDECouponLink record);

    List<UNIDECouponLink> selectByExampleWithRowbounds(UNIDECouponLinkExample example, RowBounds rowBounds);

    List<UNIDECouponLink> selectByExample(UNIDECouponLinkExample example);

    int updateByExampleSelective(@Param("record") UNIDECouponLink record, @Param("example") UNIDECouponLinkExample example);

    int updateByExample(@Param("record") UNIDECouponLink record, @Param("example") UNIDECouponLinkExample example);
}