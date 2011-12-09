package nl.strohalm.cyclos.controls.admins;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.services.transactions.DrawDownService;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StartDrawDown extends Action {

    DrawDownService drawDownService;

    @Inject
    public void setDrawDownService(DrawDownService drawDownService) {
        this.drawDownService = drawDownService;
    }

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String report = drawDownService.start();
        response.getOutputStream().write(report.getBytes());
        response.getOutputStream().close();
        return super.execute(mapping, form, request, response);
    }
}
