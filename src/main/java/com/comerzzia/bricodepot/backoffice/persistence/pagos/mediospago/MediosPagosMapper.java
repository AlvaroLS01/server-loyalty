package com.comerzzia.bricodepot.backoffice.persistence.pagos.mediospago;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MediosPagosMapper {

        List<MedioPagoRow> getMediosPago(@Param("uidActividad") String uidActividad,
                        @Param("uidTicket") String uidTicket);
}
