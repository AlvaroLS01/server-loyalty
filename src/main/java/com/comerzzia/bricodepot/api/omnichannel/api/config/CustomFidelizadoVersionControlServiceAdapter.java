package com.comerzzia.bricodepot.api.omnichannel.api.config;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.comerzzia.core.servicios.contadores.ContadorException;
import com.comerzzia.core.servicios.contadores.ServicioContadoresImpl;
import com.comerzzia.core.servicios.sesion.IDatosSesion;
import com.comerzzia.core.util.db.Database;
import com.comerzzia.model.versionado.VersionadoBean;
import com.comerzzia.servicios.fidelizacion.fidelizados.versioning.FidelizadoVersion;
import com.comerzzia.servicios.fidelizacion.fidelizados.versioning.FidelizadoVersionControlServiceImpl;
import com.comerzzia.servicios.versionado.VersionadoException;

/**
 * Spring managed adapter that exposes the existing fidelity version control logic
 * used in the backoffice so that it can be injected in the API services.
 */
@Service
@Primary
public class CustomFidelizadoVersionControlServiceAdapter extends FidelizadoVersionControlServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(CustomFidelizadoVersionControlServiceAdapter.class);

    @Override
    public void checkFidelizadosVersion(IDatosSesion datosSesion, FidelizadoVersion fidelizadoVersion) {
        log.debug("checkFidelizadosVersion() - Starting version check");

        if (fidelizadoVersion == null || fidelizadoVersion.getFidelizadosChanedList().isEmpty()) {
            return;
        }

        Long version;
        try {
            version = ServicioContadoresImpl.get().obtenerValorContador(datosSesion, CONTADOR_ID);
        } catch (ContadorException e) {
            throw new RuntimeException(e);
        }

        SqlSession sqlSession = null;
        try {
            if (fidelizadoVersion.getConnection() != null) {
                sqlSession = Database.getSqlSession(fidelizadoVersion.getConnection());
            } else {
                sqlSession = Database.getSqlSession();
            }

            for (Long idFidelizado : fidelizadoVersion.getFidelizadosChanedList()) {
                VersionadoBean versionadoBean = new VersionadoBean();
                versionadoBean.setIdClase(ID_CLASE);
                versionadoBean.setIdObjeto(idFidelizado.toString());
                versionadoBean.setVersion(version);
                try {
                    getServicioVersionado().versionar(sqlSession, datosSesion, versionadoBean);
                } catch (VersionadoException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e) {
            log.error("checkFidelizadosVersion() - Error updating loyalty customer version: {}", e.getMessage(), e);
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }
}
