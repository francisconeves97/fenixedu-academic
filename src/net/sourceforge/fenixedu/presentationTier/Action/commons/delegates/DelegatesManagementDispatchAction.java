package net.sourceforge.fenixedu.presentationTier.Action.commons.delegates;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.fenixedu.applicationTier.Servico.exceptions.FenixServiceException;
import net.sourceforge.fenixedu.applicationTier.Servico.pedagogicalCouncil.delegates.AddNewDelegate;
import net.sourceforge.fenixedu.applicationTier.Servico.pedagogicalCouncil.delegates.RemoveDelegate;
import net.sourceforge.fenixedu.dataTransferObject.pedagogicalCouncil.delegates.DelegateBean;
import net.sourceforge.fenixedu.domain.CurricularYear;
import net.sourceforge.fenixedu.domain.Degree;
import net.sourceforge.fenixedu.domain.ExecutionYear;
import net.sourceforge.fenixedu.domain.Person;
import net.sourceforge.fenixedu.domain.degree.DegreeType;
import net.sourceforge.fenixedu.domain.elections.DelegateElection;
import net.sourceforge.fenixedu.domain.elections.YearDelegateElection;
import net.sourceforge.fenixedu.domain.organizationalStructure.Function;
import net.sourceforge.fenixedu.domain.organizationalStructure.FunctionType;
import net.sourceforge.fenixedu.domain.organizationalStructure.PersonFunction;
import net.sourceforge.fenixedu.domain.student.Student;
import net.sourceforge.fenixedu.presentationTier.Action.base.FenixDispatchAction;
import net.sourceforge.fenixedu.util.BundleUtil;
import net.sourceforge.fenixedu.util.StringUtils;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.utl.ist.fenix.tools.util.excel.StyledExcelSpreadsheet;

public abstract class DelegatesManagementDispatchAction extends FenixDispatchAction {

    protected static final String RESOURCE_MODULE = "pedagogicalCouncil";

    @Override
    protected Object getFromRequest(HttpServletRequest request, String id) {
	if (RenderUtils.getViewState(id) != null)
	    return RenderUtils.getViewState(id).getMetaObject().getObject();
	else if (request.getParameter(id) != null)
	    return request.getParameter(id);
	else
	    return request.getAttribute(id);
    }

    /*
     * Degree Delegates Group Management
     */
    public ActionForward prepare(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();

	DelegateBean bean = (DelegateBean) getFromRequest(request, "delegateBean");

	if (bean == null) {
	    bean = new DelegateBean();
	    bean.setDegreeType(DegreeType.BOLONHA_DEGREE);
	    bean.setDegree(getDefaultDegreeGivenDegreeType(DegreeType.BOLONHA_DEGREE));
	} else {
	    RenderUtils.invalidateViewState("delegateBean");
	    bean.setDegree(getDefaultDegreeGivenDegreeType(bean.getDegreeType()));
	}

	request.setAttribute("delegateBean", bean);
	request.setAttribute("currentExecutionYear", currentExecutionYear);
	return prepareViewDelegates(mapping, actionForm, request, response);
    }

    public ActionForward prepareSelectDegree(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();

	DelegateBean bean = (DelegateBean) getFromRequest(request, "delegateBean");
	RenderUtils.invalidateViewState("delegateBean");

	if (bean.getDegree() == null) {
	    bean.setDegree(getDefaultDegreeGivenDegreeType(bean.getDegreeType()));
	}

	request.setAttribute("delegateBean", bean);
	request.setAttribute("currentExecutionYear", currentExecutionYear);
	return prepareViewDelegates(mapping, actionForm, request, response);
    }

    public ActionForward prepareViewDelegates(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();

	DelegateBean bean = (DelegateBean) getFromRequest(request, "delegateBean");

	final Degree degree = bean.getDegree();
	final DegreeType degreeType = bean.getDegreeType();

	List<DelegateBean> delegates = new ArrayList<DelegateBean>();

	if (degreeType.equals(DegreeType.BOLONHA_INTEGRATED_MASTER_DEGREE)) {
	    DelegateBean delegateBean = getDelegateBean(degree, FunctionType.DELEGATE_OF_INTEGRATED_MASTER_DEGREE);
	    delegates.add(delegateBean);
	}

	if (degreeType.isSecondCycle()) {
	    DelegateBean delegateBean = getDelegateBean(degree, FunctionType.DELEGATE_OF_MASTER_DEGREE);
	    delegates.add(delegateBean);
	}

	if (degreeType.isFirstCycle()) {
	    DelegateBean delegateBean = getDelegateBean(degree, FunctionType.DELEGATE_OF_DEGREE);
	    delegates.add(delegateBean);
	}

	delegates.addAll(getYearDelegateBeans(degree));

	request.setAttribute("delegates", delegates);
	request.setAttribute("delegateBean", bean);
	request.setAttribute("currentExecutionYear", currentExecutionYear);
	return mapping.findForward("createEditDelegates");
    }

    public ActionForward exportToXLS(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws FenixServiceException {

	ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
	try {
	    String filename = getResourceMessage("delegates.section") + "_" + currentExecutionYear.getYear();

	    response.setContentType("application/vnd.ms-excel");
	    response.setHeader("Content-disposition", "attachment; filename=" + filename + ".xls");
	    ServletOutputStream writer = response.getOutputStream();

	    exportDelegatesToXLS(currentExecutionYear, writer);
	    writer.flush();
	    response.flushBuffer();
	    return null;

	} catch (IOException e) {
	    throw new FenixServiceException();
	}
    }

    private void exportDelegatesToXLS(ExecutionYear executionYear, OutputStream outputStream) throws IOException {

	final StyledExcelSpreadsheet spreadsheet = new StyledExcelSpreadsheet(getResourceMessage("delegates.section"));
	fillSpreadSheetHeaders(spreadsheet);
	fillSpreadSheetResults(executionYear, spreadsheet);
	spreadsheet.getWorkbook().write(outputStream);
    }

    private void fillSpreadSheetHeaders(final StyledExcelSpreadsheet spreadsheet) {
	spreadsheet.newHeaderRow();
	spreadsheet.addHeader(getResourceMessage("label.degreeType"));
	spreadsheet.addHeader(getResourceMessage("label.degree"));
	spreadsheet.addHeader(getResourceMessage("label.functionType"));
	spreadsheet.addHeader(getResourceMessage("label.curricularYear"));
	spreadsheet.addHeader(getResourceMessage("label.studentNumber"));
	spreadsheet.addHeader(getResourceMessage("label.name"));
	spreadsheet.addHeader(getResourceMessage("label.phone"));
	spreadsheet.addHeader(getResourceMessage("label.email"));
	spreadsheet.addHeader(getResourceMessage("label.address"));
    }

    private void fillSpreadSheetResults(ExecutionYear executionYear, final StyledExcelSpreadsheet spreadsheet) {
	List<Degree> degrees = new ArrayList<Degree>();
	degrees.addAll(Degree.readAllByDegreeType(DegreeType.BOLONHA_DEGREE));
	degrees.addAll(Degree.readAllByDegreeType(DegreeType.BOLONHA_MASTER_DEGREE));
	degrees.addAll(Degree.readAllByDegreeType(DegreeType.BOLONHA_INTEGRATED_MASTER_DEGREE));

	Map<DegreeType, FunctionType> delegateTypeByDegreeType = new HashMap<DegreeType, FunctionType>();
	delegateTypeByDegreeType.put(DegreeType.BOLONHA_DEGREE, FunctionType.DELEGATE_OF_DEGREE);
	delegateTypeByDegreeType.put(DegreeType.BOLONHA_MASTER_DEGREE, FunctionType.DELEGATE_OF_MASTER_DEGREE);
	delegateTypeByDegreeType.put(DegreeType.BOLONHA_INTEGRATED_MASTER_DEGREE,
		FunctionType.DELEGATE_OF_INTEGRATED_MASTER_DEGREE);

	for (Degree degree : degrees) {
	    List<DelegateBean> delegateBeans = new ArrayList<DelegateBean>();
	    delegateBeans.add(getDelegateBean(degree, delegateTypeByDegreeType.get(degree.getDegreeType())));
	    delegateBeans.addAll(getYearDelegateBeans(degree));

	    for (DelegateBean delegateBean : delegateBeans) {
		Person delegate = (delegateBean.getDelegate() == null) ? null : delegateBean.getDelegate().getPerson();
		spreadsheet.newRow();
		spreadsheet.addCell(getEnumName(degree.getDegreeType()));
		spreadsheet.addCell(degree.getNameFor(executionYear));
		spreadsheet.addCell(getEnumName(delegateBean.getDelegateType()));
		String year = (delegateBean.getCurricularYear() == null) ? "-" : String.valueOf(delegateBean.getCurricularYear()
			.getYear());
		String number = (delegateBean.getStudentNumber() == null) ? "-" : String.valueOf(delegateBean.getStudentNumber());
		String name = (StringUtils.isEmpty(delegateBean.getStudentName())) ? "-" : delegateBean.getStudentName();
		String phone = "-";
		String email = "-";
		String address = "-";
		if (delegate != null) {
		    phone = (StringUtils.isEmpty(delegate.getDefaultPhoneNumber())) ? "-" : delegate.getDefaultPhoneNumber();
		    email = (StringUtils.isEmpty(delegate.getDefaultEmailAddressValue())) ? "-" : delegate
			    .getDefaultEmailAddressValue();
		    address = (StringUtils.isEmpty(delegate.getAddress())) ? "-" : delegate.getAddress();
		}
		spreadsheet.addCell(year);
		spreadsheet.addCell(number);
		spreadsheet.addCell(name);
		spreadsheet.addCell(phone);
		spreadsheet.addCell(email);
		spreadsheet.addCell(address);
	    }
	}
    }

    protected static String getResourceMessage(String key) {
	return BundleUtil.getMessageFromModuleOrApplication(RESOURCE_MODULE, key);
    }

    protected static String getEnumName(Enum<?> enumeration) {
	return BundleUtil.getEnumName(enumeration);
    }

    public ActionForward prepareAddDelegate(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	Integer degreeOID = Integer.parseInt(request.getParameter("selectedDegree"));
	final Degree degree = rootDomainObject.readDegreeByOID(degreeOID);

	final String delegateType = request.getParameter("delegateType");
	final FunctionType delegateFunctionType = FunctionType.valueOf(delegateType);

	DelegateBean bean = getInitializedBean(degree);
	bean.setDelegateType(delegateFunctionType);

	if (delegateFunctionType.equals(FunctionType.DELEGATE_OF_YEAR)) {
	    final Integer year = Integer.parseInt(request.getParameter("selectedYear"));
	    final CurricularYear curricularYear = CurricularYear.readByYear(year);
	    bean.setCurricularYear(curricularYear);
	    request.setAttribute("newYearDelegateBean", bean);
	} else {
	    request.setAttribute("newDelegateBean", bean);
	}

	request.setAttribute("delegateBean", bean);
	return prepareViewDelegates(mapping, actionForm, request, response);
    }

    public ActionForward addYearDelegate(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	DelegateBean bean = (DelegateBean) getFromRequest(request, "newYearDelegateBean");

	if (bean != null) { /* From Delegates management main page */
	    final Integer studentNumber = bean.getStudentNumber();
	    final Student student = Student.readStudentByNumber(studentNumber);

	    if (student == null) {
		addActionMessage(request, "error.delegates.studentNotFound");
		RenderUtils.invalidateViewState("delegateBean");
		request.setAttribute("delegateBean", bean);
		return prepareViewDelegates(mapping, actionForm, request, response);
	    }
	    try {
		AddNewDelegate.run(student, bean.getCurricularYear(), bean.getDegree());
	    } catch (FenixServiceException ex) {
		addActionMessage(request, ex.getMessage(), ex.getArgs());
	    }
	} else { /* From selected delegate election voting results */
	    final Integer electionOID = Integer.parseInt(request.getParameter("selectedElection"));
	    final YearDelegateElection election = (YearDelegateElection) rootDomainObject.readDelegateElectionByOID(electionOID);
	    final Integer studentOID = Integer.parseInt(request.getParameter("selectedStudent"));
	    final Student student = rootDomainObject.readStudentByOID(studentOID);

	    bean = getInitializedBean(election.getDegree());
	    try {
		AddNewDelegate.run(student, election);
	    } catch (FenixServiceException ex) {
		addActionMessage(request, ex.getMessage(), ex.getArgs());
	    }
	}
	RenderUtils.invalidateViewState("delegateBean");
	request.setAttribute("delegateBean", bean);
	return mapping.findForward("prepareViewDelegates");
    }

    public ActionForward addDelegate(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	DelegateBean bean = (DelegateBean) getFromRequest(request, "newDelegateBean");

	final Integer studentNumber = bean.getStudentNumber();
	final Student student = Student.readStudentByNumber(studentNumber);

	if (student == null) {
	    addActionMessage(request, "error.delegates.studentNotFound");
	    RenderUtils.invalidateViewState("delegateBean");
	    request.setAttribute("delegateBean", bean);
	    return prepareViewDelegates(mapping, actionForm, request, response);
	}

	final Degree degree = bean.getDegree();
	final FunctionType functionType = bean.getDelegateType();
	try {
	    AddNewDelegate.run(student, degree, functionType);
	} catch (FenixServiceException ex) {
	    addActionMessage(request, ex.getMessage(), ex.getArgs());
	}
	RenderUtils.invalidateViewState("delegateBean");
	request.setAttribute("delegateBean", bean);
	return mapping.findForward("prepareViewDelegates");
    }

    public ActionForward removeDelegate(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	final Integer delegateOID = Integer.parseInt(request.getParameter("selectedDelegate"));
	final Student student = rootDomainObject.readStudentByOID(delegateOID);

	try {
	    if (request.getParameter("delegateType") != null) {
		final String delegateType = request.getParameter("delegateType");
		final FunctionType delegateFunctionType = FunctionType.valueOf(delegateType);

		RemoveDelegate.run(student, delegateFunctionType);
	    } else {
		RemoveDelegate.run(student);
	    }
	} catch (FenixServiceException ex) {
	    addActionMessage(request, ex.getMessage(), ex.getArgs());
	}

	DelegateBean bean = getInitializedBean(student.getLastActiveRegistration().getDegree());

	request.setAttribute("delegateBean", bean);
	return prepareViewDelegates(mapping, actionForm, request, response);
    }

    public ActionForward prepareViewResults(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();

	Integer degreeOID = Integer.parseInt(request.getParameter("selectedDegree"));
	final Degree degree = rootDomainObject.readDegreeByOID(degreeOID);

	Integer year = Integer.parseInt(request.getParameter("selectedYear"));
	final CurricularYear curricularYear = CurricularYear.readByYear(year);

	DelegateBean bean = getInitializedBean(degree);

	DelegateElection election = degree.getYearDelegateElectionWithLastCandidacyPeriod(currentExecutionYear, curricularYear);
	if (election == null) {
	    addActionMessage(request, "error.delegates.noElection");
	    request.setAttribute("delegateBean", bean);
	    return prepareViewDelegates(mapping, actionForm, request, response);
	}

	request.setAttribute("selectedVotingPeriod", election.getIdInternal().toString());
	return mapping.findForward("showPossibleDelegates");
    }

    public ActionForward prepareChangeDelegate(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	final Integer delegateOID = Integer.parseInt(request.getParameter("selectedDelegate"));
	final Student delegate = rootDomainObject.readStudentByOID(delegateOID);

	DelegateElection election = delegate.getLastElectedDelegateElection();

	request.setAttribute("selectedVotingPeriod", election.getIdInternal().toString());
	return mapping.findForward("showPossibleDelegates");
    }

    public ActionForward goBackToViewDelegates(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	final Integer electionOID = Integer.parseInt(request.getParameter("selectedElection"));
	final DelegateElection election = rootDomainObject.readDelegateElectionByOID(electionOID);

	DelegateBean delegateBean = getInitializedBean(election.getDegree());
	request.setAttribute("delegateBean", delegateBean);

	return prepareViewDelegates(mapping, actionForm, request, response);
    }

    /*
     * GGAE Delegates Management
     */
    public ActionForward prepareViewGGAEDelegates(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();

	List<DelegateBean> ggaeDelegates = getGGAEDelegateBeans();
	Collections.sort(ggaeDelegates, new BeanComparator("ggaeDelegateFunction.name"));

	request.setAttribute("ggaeDelegates", ggaeDelegates);
	request.setAttribute("currentExecutionYear", currentExecutionYear);
	return mapping.findForward("createEditGGAEDelegates");
    }

    public ActionForward prepareAddGGAEDelegate(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
	request.setAttribute("currentExecutionYear", currentExecutionYear);

	DelegateBean bean = (DelegateBean) getFromRequest(request, "delegateBean");
	if (bean == null) {
	    final Integer functionId = Integer.parseInt(request.getParameter("selectedGgaeFunction"));
	    final Function function = (Function) rootDomainObject.readAccountabilityTypeByOID(functionId);

	    bean = new DelegateBean();
	    bean.setGgaeDelegateFunction(function);

	    request.setAttribute("choosePersonBean", bean);
	    return prepareViewGGAEDelegates(mapping, actionForm, request, response);
	}

	if (bean.getPersonUsername() == null) {
	    request.setAttribute("chooseStudentBean", bean);
	    return prepareViewGGAEDelegates(mapping, actionForm, request, response);
	}

	Person person = Person.readPersonByUsername(bean.getPersonUsername());

	if (person != null) {
	    bean.setGgaeDelegate(person);
	    RenderUtils.invalidateViewState("delegateBean");
	    request.setAttribute("confirmPersonBean", bean);
	} else {
	    addActionMessage(request, "error.delegates.personNotFound");
	    RenderUtils.invalidateViewState("delegateBean");
	    request.setAttribute("choosePersonBean", bean);
	}
	return prepareViewGGAEDelegates(mapping, actionForm, request, response);
    }

    public ActionForward addGGAEDelegate(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	DelegateBean bean = (DelegateBean) getFromRequest(request, "delegateBean");

	final Person person = bean.getGgaeDelegate();

	final Function delegateFunction = bean.getGgaeDelegateFunction();
	AddNewDelegate.run(person, delegateFunction);
	RenderUtils.invalidateViewState("delegateBean");
	request.setAttribute("delegateBean", bean);
	return mapping.findForward("prepareViewGGAEDelegates");
    }

    public ActionForward removeGGAEDelegate(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	final Integer delegateOID = Integer.parseInt(request.getParameter("selectedDelegate"));
	final Person person = (Person) rootDomainObject.readPartyByOID(delegateOID);

	final Integer functionId = Integer.parseInt(request.getParameter("selectedGgaeFunction"));
	final Function function = (Function) rootDomainObject.readAccountabilityTypeByOID(functionId);

	try {
	    RemoveDelegate.run(person, function);
	} catch (FenixServiceException ex) {
	    addActionMessage(request, ex.getMessage(), ex.getArgs());
	}

	return prepareViewGGAEDelegates(mapping, actionForm, request, response);
    }

    /*
     * AUXILIARY METHODS
     */

    /* Delegates from given degree (not year delegates) */
    private DelegateBean getDelegateBean(Degree degree, FunctionType functionType) {
	DelegateBean delegateBean = getInitializedBean(degree);
	delegateBean.setDelegateType(functionType);
	List<Student> delegates = degree.getAllActiveDelegatesByFunctionType(functionType, delegateBean.getExecutionYear());
	if (!delegates.isEmpty()) {
	    delegateBean.setDelegate(delegates.get(0));
	}
	return delegateBean;
    }

    private List<DelegateBean> getYearDelegateBeans(Degree degree) {
	List<DelegateBean> yearDelegates = new ArrayList<DelegateBean>();
	for (int i = 1; i <= degree.getDegreeType().getYears(); i++) {
	    final CurricularYear curricularYear = CurricularYear.readByYear(i);
	    final Student student = degree.getActiveYearDelegateByCurricularYear(curricularYear); // can
	    // be
	    // null
	    // if
	    // doesn
	    // 't
	    // exist
	    final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
	    final DelegateElection election = degree
		    .getYearDelegateElectionWithLastCandidacyPeriod(executionYear, curricularYear);

	    DelegateBean delegateBean = getInitializedBean(degree);
	    delegateBean.setDelegate(student);
	    delegateBean.setDelegateType(FunctionType.DELEGATE_OF_YEAR);
	    delegateBean.setCurricularYear(curricularYear);
	    delegateBean.setDelegateElection(election);
	    yearDelegates.add(delegateBean);
	}
	return yearDelegates;
    }

    private DelegateBean getInitializedBean(final Degree degree) {
	DelegateBean bean = new DelegateBean();
	bean.setDegreeType(degree.getDegreeType());
	bean.setDegree(degree);
	return bean;
    }

    private Degree getDefaultDegreeGivenDegreeType(DegreeType degreeType) {
	List<Degree> degrees = Degree.readAllByDegreeType(degreeType);
	return degrees.get(0);
    }

    private List<DelegateBean> getGGAEDelegateBeans() {
	final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
	List<DelegateBean> result = new ArrayList<DelegateBean>();
	Set<Function> functions = Function.readAllActiveFunctionsByType(FunctionType.DELEGATE_OF_GGAE);
	for (Function function : functions) {
	    if (function.getActivePersonFunctionsStartingIn(executionYear).isEmpty()) {
		DelegateBean bean = new DelegateBean();
		bean.setDelegateType(FunctionType.DELEGATE_OF_GGAE);
		bean.setGgaeDelegateFunction(function);
		result.add(bean);
	    } else {
		for (PersonFunction personFunction : function.getActivePersonFunctionsStartingIn(executionYear)) {
		    DelegateBean bean = new DelegateBean();
		    bean.setGgaeDelegate(personFunction.getPerson());
		    bean.setDelegateType(FunctionType.DELEGATE_OF_GGAE);
		    bean.setGgaeDelegateFunction(function);
		    result.add(bean);
		}
	    }
	}
	return result;
    }
}
