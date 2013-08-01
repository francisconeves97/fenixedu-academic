package net.sourceforge.fenixedu.presentationTier.Action.academicAdministration.payments;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.fenixedu.applicationTier.Servico.accounting.AnnulAccountingTransaction;
import net.sourceforge.fenixedu.applicationTier.Servico.accounting.AnnulReceipt;
import net.sourceforge.fenixedu.applicationTier.Servico.accounting.CancelEvent;
import net.sourceforge.fenixedu.applicationTier.Servico.accounting.DepositAmountOnEvent;
import net.sourceforge.fenixedu.applicationTier.Servico.accounting.OpenEvent;
import net.sourceforge.fenixedu.applicationTier.Servico.accounting.TransferPaymentsToOtherEventAndCancel;
import net.sourceforge.fenixedu.applicationTier.Servico.exceptions.FenixServiceException;
import net.sourceforge.fenixedu.applicationTier.Servico.person.SearchPerson;
import net.sourceforge.fenixedu.applicationTier.Servico.person.SearchPerson.SearchParameters;
import net.sourceforge.fenixedu.applicationTier.Servico.person.SearchPerson.SearchPersonPredicate;
import net.sourceforge.fenixedu.dataTransferObject.accounting.AnnulAccountingTransactionBean;
import net.sourceforge.fenixedu.dataTransferObject.accounting.CancelEventBean;
import net.sourceforge.fenixedu.dataTransferObject.accounting.DepositAmountBean;
import net.sourceforge.fenixedu.dataTransferObject.accounting.TransferPaymentsToOtherEventAndCancelBean;
import net.sourceforge.fenixedu.dataTransferObject.person.SimpleSearchPersonWithStudentBean;
import net.sourceforge.fenixedu.domain.ExecutionInterval;
import net.sourceforge.fenixedu.domain.Person;
import net.sourceforge.fenixedu.domain.User;
import net.sourceforge.fenixedu.domain.accounting.AccountingTransaction;
import net.sourceforge.fenixedu.domain.accounting.Discount;
import net.sourceforge.fenixedu.domain.accounting.Event;
import net.sourceforge.fenixedu.domain.accounting.PaymentCodeMapping;
import net.sourceforge.fenixedu.domain.accounting.PaymentCodeMapping.PaymentCodeMappingBean;
import net.sourceforge.fenixedu.domain.accounting.Receipt;
import net.sourceforge.fenixedu.domain.exceptions.DomainException;
import net.sourceforge.fenixedu.domain.exceptions.DomainExceptionWithLabelFormatter;
import net.sourceforge.fenixedu.presentationTier.Action.base.FenixDispatchAction;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.utl.ist.fenix.tools.util.CollectionPager;

public class PaymentsManagementDA extends FenixDispatchAction {

    public ActionForward prepareSearchPerson(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        request.setAttribute("searchPersonBean", new SimpleSearchPersonWithStudentBean());

        return mapping.findForward("searchPersons");
    }

    public ActionForward prepareSearchPersonInvalid(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        request.setAttribute("searchPersonBean", getObjectFromViewState("searchPersonBean"));

        return mapping.findForward("searchPersons");
    }

    public ActionForward searchPerson(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws  FenixServiceException {
        final SimpleSearchPersonWithStudentBean searchPersonBean =
                (SimpleSearchPersonWithStudentBean) getObjectFromViewState("searchPersonBean");
        request.setAttribute("searchPersonBean", searchPersonBean);

        final Collection<Person> persons = searchPerson(request, searchPersonBean);
        if (persons.size() == 1) {
            request.setAttribute("personId", persons.iterator().next().getIdInternal());

            return showOperations(mapping, form, request, response);

        }

        request.setAttribute("persons", persons);
        return mapping.findForward("searchPersons");
    }

    public ActionForward showEvents(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws  FenixServiceException {

        request.setAttribute("person", getPerson(request));

        return mapping.findForward("showEvents");

    }

    public ActionForward showPaymentsForEvent(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        final Event event = getEvent(request);
        request.setAttribute("event", event);

        if (!StringUtils.isEmpty(event.getCreatedBy())) {
            User responsible = User.readUserByUserUId(event.getCreatedBy());
            request.setAttribute("responsible", responsible.getPerson());
        }

        if (event.isOpen()) {
            request.setAttribute("entryDTOs", event.calculateEntries());
            request.setAttribute("accountingEventPaymentCodes", event.getNonProcessedPaymentCodes());
        }

        return mapping.findForward("showPaymentsForEvent");
    }

    protected Collection<Person> searchPerson(HttpServletRequest request, final SimpleSearchPersonWithStudentBean searchPersonBean)
            throws  FenixServiceException {
        final SearchParameters searchParameters =
                new SearchPerson.SearchParameters(searchPersonBean.getName(), null, searchPersonBean.getUsername(),
                        searchPersonBean.getDocumentIdNumber(), searchPersonBean.getIdDocumentType() != null ? searchPersonBean
                                .getIdDocumentType().toString() : null, null, null, null, null, null,
                        searchPersonBean.getStudentNumber(), Boolean.FALSE, searchPersonBean.getPaymentCode());

        final SearchPersonPredicate predicate = new SearchPerson.SearchPersonPredicate(searchParameters);

        final CollectionPager<Person> result =
                (CollectionPager<Person>) SearchPerson.runSearchPerson( searchParameters, predicate );

        return result.getCollection();

    }

    public ActionForward prepareCancelEvent(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        request.setAttribute("cancelEventBean", new CancelEventBean(getEvent(request), getLoggedPerson(request)));

        return mapping.findForward("editCancelEventJustification");
    }

    public ActionForward cancelEvent(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws  FenixServiceException {

        final CancelEventBean cancelEventBean = getCancelEventBean();

        try {
            CancelEvent.run(cancelEventBean.getEvent(), cancelEventBean.getResponsible(), cancelEventBean.getJustification());
        } catch (DomainExceptionWithLabelFormatter ex) {

            addActionMessage(request, ex.getKey(), solveLabelFormatterArgs(request, ex.getLabelFormatterArgs()));
            request.setAttribute("cancelEventBean", cancelEventBean);

            return mapping.findForward("editCancelEventJustification");
        } catch (DomainException ex) {

            addActionMessage(request, ex.getKey(), ex.getArgs());
            request.setAttribute("cancelEventBean", cancelEventBean);

            return mapping.findForward("editCancelEventJustification");
        }

        request.setAttribute("person", cancelEventBean.getEvent().getPerson());

        return mapping.findForward("showEvents");

    }

    public ActionForward openEvent(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws  FenixServiceException {
        try {
            OpenEvent.run(getEvent(request));
        } catch (DomainExceptionWithLabelFormatter ex) {
            addActionMessage(request, ex.getKey(), solveLabelFormatterArgs(request, ex.getLabelFormatterArgs()));
        } catch (DomainException ex) {
            addActionMessage(request, ex.getKey(), ex.getArgs());
        }

        request.setAttribute("personId", getEvent(request).getPerson().getIdInternal());

        return showEvents(mapping, form, request, response);

    }

    private CancelEventBean getCancelEventBean() {
        final CancelEventBean cancelEventBean = (CancelEventBean) getObjectFromViewState("cancelEventBean");
        return cancelEventBean;
    }

    public ActionForward backToShowEvents(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        request.setAttribute("person", getPerson(request));

        return mapping.findForward("showEvents");

    }

    protected Person getPerson(HttpServletRequest request) {
        return (Person) rootDomainObject.readPartyByOID(getIntegerFromRequest(request, "personId"));
    }

    private Event getEvent(HttpServletRequest request) {
        return rootDomainObject.readEventByOID(getIntegerFromRequest(request, "eventId"));
    }

    public ActionForward prepareTransferPaymentsToOtherEventAndCancel(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) {

        final Event event = getEvent(request);

        final TransferPaymentsToOtherEventAndCancelBean transferPaymentsBean =
                new TransferPaymentsToOtherEventAndCancelBean(event, getLoggedPerson(request));

        request.setAttribute("transferPaymentsBean", transferPaymentsBean);

        return mapping.findForward("chooseTargetEventForPaymentsTransfer");

    }

    public ActionForward transferPaymentsToOtherEventAndCancel(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws  FenixServiceException {

        final TransferPaymentsToOtherEventAndCancelBean transferPaymentsBean =
                (TransferPaymentsToOtherEventAndCancelBean) getObjectFromViewState("transferPaymentsBean");

        try {
            TransferPaymentsToOtherEventAndCancel.run(transferPaymentsBean.getResponsible(),
                    transferPaymentsBean.getSourceEvent(), transferPaymentsBean.getTargetEvent(),
                    transferPaymentsBean.getCancelJustification());
        } catch (DomainExceptionWithLabelFormatter ex) {

            addActionMessage(request, ex.getKey(), solveLabelFormatterArgs(request, ex.getLabelFormatterArgs()));
            request.setAttribute("transferPaymentsBean", transferPaymentsBean);

            return mapping.findForward("chooseTargetEventForPaymentsTransfer");
        } catch (DomainException ex) {

            addActionMessage(request, ex.getKey(), ex.getArgs());
            request.setAttribute("transferPaymentsBean", transferPaymentsBean);

            return mapping.findForward("chooseTargetEventForPaymentsTransfer");
        }

        request.setAttribute("event", transferPaymentsBean.getSourceEvent());

        return mapping.findForward("showPaymentsForEvent");

    }

    public ActionForward prepareAnnulTransaction(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        request.setAttribute("annulAccountingTransactionBean", new AnnulAccountingTransactionBean(getTransaction(request)));

        return mapping.findForward("annulTransaction");

    }

    public ActionForward prepareAnnulTransactionInvalid(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        request.setAttribute("annulAccountingTransactionBean", getObjectFromViewState("annulAccountingTransactionBean"));

        return mapping.findForward("annulTransaction");
    }

    public ActionForward annulTransaction(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws  FenixServiceException {
        final AnnulAccountingTransactionBean annulAccountingTransactionBean =
                (AnnulAccountingTransactionBean) getObjectFromViewState("annulAccountingTransactionBean");
        try {

            AnnulAccountingTransaction.run(getLoggedPerson(request).getEmployee(), annulAccountingTransactionBean);
        } catch (DomainExceptionWithLabelFormatter ex) {

            addActionMessage(request, ex.getKey(), solveLabelFormatterArgs(request, ex.getLabelFormatterArgs()));
            request.setAttribute("annulAccountingTransactionBean", annulAccountingTransactionBean);

            return mapping.findForward("annulTransaction");
        } catch (DomainException ex) {

            addActionMessage(request, ex.getKey(), ex.getArgs());
            request.setAttribute("annulAccountingTransactionBean", annulAccountingTransactionBean);

            return mapping.findForward("annulTransaction");
        }

        return showEvents(mapping, form, request, response);
    }

    private AccountingTransaction getTransaction(HttpServletRequest request) {
        return rootDomainObject.readAccountingTransactionByOID(getIntegerFromRequest(request, "transactionId"));
    }

    public ActionForward showOperations(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        request.setAttribute("permission", true);
        request.setAttribute("person", getPerson(request));

        return mapping.findForward("showOperations");
    }

    public ActionForward showReceipts(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        request.setAttribute("person", getPerson(request));

        return mapping.findForward("showReceipts");
    }

    public ActionForward showReceipt(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        request.setAttribute("receipt", getReceipt(request));

        return mapping.findForward("showReceipt");
    }

    public ActionForward annulReceipt(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws  FenixServiceException {

        try {
            AnnulReceipt.run(getLoggedPerson(request), getReceipt(request));
        } catch (DomainExceptionWithLabelFormatter ex) {
            addActionMessage(request, ex.getKey(), solveLabelFormatterArgs(request, ex.getLabelFormatterArgs()));
        } catch (DomainException ex) {
            addActionMessage(request, ex.getKey(), ex.getArgs());
        }

        return showReceipts(mapping, form, request, response);
    }

    private Receipt getReceipt(HttpServletRequest request) {
        return rootDomainObject.readReceiptByOID(getIntegerFromRequest(request, "receiptId"));
    }

    @Override
    protected Map<String, String> getMessageResourceProviderBundleMappings() {
        final Map<String, String> bundleMappings = new HashMap<String, String>();
        bundleMappings.put("enum", "ENUMERATION_RESOURCES");
        bundleMappings.put("application", "DEFAULT");
        return bundleMappings;
    }

    public ActionForward prepareDepositAmount(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        request.setAttribute("depositAmountBean", new DepositAmountBean(getEvent(request)));

        return mapping.findForward("depositAmount");
    }

    public ActionForward prepareDepositAmountInvalid(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        request.setAttribute("depositAmountBean", getRenderedObject("depositAmountBean"));

        return mapping.findForward("depositAmount");
    }

    public ActionForward depositAmount(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws  FenixServiceException {

        final DepositAmountBean renderedObject = getRenderedObject("depositAmountBean");
        try {
            DepositAmountOnEvent.run(renderedObject);
        } catch (DomainException e) {
            addActionMessage(request, e.getKey(), e.getArgs());

            request.setAttribute("depositAmountBean", renderedObject);

            return mapping.findForward("depositAmount");
        }

        return showEvents(mapping, form, request, response);
    }

    public ActionForward prepareViewPaymentCodeMappings(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        request.setAttribute("paymentCodeMappingBean", new PaymentCodeMappingBean());
        return mapping.findForward("viewCodes");
    }

    public ActionForward viewPaymentCodeMappings(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {

        final PaymentCodeMappingBean bean = getRenderedObject("paymentCodeMappingBean");
        request.setAttribute("paymentCodeMappingBean", bean);

        if (bean.hasExecutionInterval()) {
            request.setAttribute("paymentCodeMappings", bean.getExecutionInterval().getPaymentCodeMappings());
        }

        return mapping.findForward("viewCodes");
    }

    public ActionForward prepareCreatePaymentCodeMapping(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) {
        RenderUtils.invalidateViewState();
        final PaymentCodeMappingBean bean = new PaymentCodeMappingBean();
        bean.setExecutionInterval((ExecutionInterval) getDomainObject(request, "executionIntervalOid"));
        request.setAttribute("paymentCodeMappingBean", bean);
        return mapping.findForward("createPaymentCodeMapping");
    }

    public ActionForward createPaymentCodeMapping(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {

        final PaymentCodeMappingBean bean = getRenderedObject("paymentCodeMappingBean");
        request.setAttribute("paymentCodeMappingBean", bean);

        try {
            bean.create();
        } catch (final DomainException e) {
            addActionMessage(request, e.getKey(), e.getArgs());
            return mapping.findForward("createPaymentCodeMapping");
        }

        request.setAttribute("paymentCodeMappings", bean.getExecutionInterval().getPaymentCodeMappings());
        RenderUtils.invalidateViewState();
        bean.clear();
        return mapping.findForward("viewCodes");
    }

    public ActionForward createPaymentCodeMappingInvalid(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) {
        request.setAttribute("paymentCodeMappingBean", getRenderedObject("paymentCodeMappingBean"));
        return mapping.findForward("createPaymentCodeMapping");
    }

    public ActionForward deletePaymentCodeMapping(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {

        final PaymentCodeMapping codeMapping = getDomainObject(request, "paymentCodeMappingOid");
        final PaymentCodeMappingBean bean = new PaymentCodeMappingBean();
        bean.setExecutionInterval(codeMapping.getExecutionInterval());

        request.setAttribute("paymentCodeMappingBean", bean);
        request.setAttribute("paymentCodeMappings", bean.getExecutionInterval().getPaymentCodeMappings());

        try {
            codeMapping.delete();
        } catch (final DomainException e) {
            addActionMessage(request, e.getMessage(), e.getArgs());
        }

        return mapping.findForward("viewCodes");
    }

    public ActionForward prepareChangePaymentPlan(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        request.setAttribute("event", getEvent(request));
        return mapping.findForward("changePaymentPlan");
    }

    public ActionForward deleteDiscount(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {

        final Discount discount = getDomainObject(request, "discountOid");
        request.setAttribute("eventId", discount.getEvent().getIdInternal());

        try {
            discount.delete();
        } catch (final DomainException e) {
            addActionMessage(request, e.getMessage(), e.getArgs());
        }

        return showPaymentsForEvent(mapping, actionForm, request, response);
    }

    public ActionForward prepareViewEventsToCancel(final ActionMapping mapping, final ActionForm actionForm,
            final HttpServletRequest request, final HttpServletResponse response) {
        Person person = getDomainObject(request, "personId");
        request.setAttribute("person", person);

        return mapping.findForward("viewEventsForCancellation");
    }

}