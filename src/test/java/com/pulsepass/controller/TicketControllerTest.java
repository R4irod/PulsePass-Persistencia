package com.pulsepass.controller;

import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.TicketService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    private static final String VALID_PURCHASE_JSON = """
            {
              "userEmail": "andrea@email.com",
              "eventCode": "CMF-2026",
              "type": "VIP"
            }
            """;

    private TicketResponse ticket(String code, TicketStatus status) {
        return new TicketResponse(
                1L, code, TicketType.VIP, new BigDecimal("250000.00"), status,
                LocalDateTime.of(2026, 10, 9, 12, 0),
                "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026");
    }

    // TEST-CTRL-TKT-001 — compra válida -> 201 (AC-CTRL-009)
    @Test
    void shouldReturn201WhenPurchaseIsValid() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenReturn(ticket("TCK-0001", TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PURCHASE_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-0001"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.type").value("VIP"))
                .andExpect(jsonPath("$.userEmail").value("andrea@email.com"))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.price").exists());

        ArgumentCaptor<PurchaseTicketRequest> captor = ArgumentCaptor.forClass(PurchaseTicketRequest.class);
        verify(ticketService).purchase(captor.capture());
        assertThat(captor.getValue().userEmail()).isEqualTo("andrea@email.com");
        assertThat(captor.getValue().eventCode()).isEqualTo("CMF-2026");
        assertThat(captor.getValue().type()).isEqualTo(TicketType.VIP);
    }

    // TEST-CTRL-TKT-002 — request inválido -> 400; el Service NO se invoca
    @Test
    void shouldReturn400AndNotCallServiceWhenPurchaseRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.userEmail").value("User email is required"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    void shouldReturn400WhenPurchaseEmailIsInvalid() throws Exception {
        String json = VALID_PURCHASE_JSON.replace("andrea@email.com", "not-an-email");

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.userEmail").value("User email must be valid"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    void shouldReturn400WhenTicketTypeIsUnknown() throws Exception {
        String json = VALID_PURCHASE_JSON.replace("\"VIP\"", "\"PLATINUM\"");

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));

        verify(ticketService, never()).purchase(any());
    }

    // TEST-CTRL-TKT-003 — usuario inexistente -> 404
    @Test
    void shouldReturn404WhenPurchasingWithUnknownUser() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@email.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PURCHASE_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: andrea@email.com"));
    }

    // TEST-CTRL-TKT-004 — regla de negocio (p. ej. menor de edad, AC-CTRL-010) -> 409
    @Test
    void shouldReturn409WhenPurchaseViolatesBusinessRule() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("User does not meet minimum age."));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PURCHASE_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User does not meet minimum age."));
    }

    // TEST-CTRL-TKT-005 — consultar ticket -> 200
    @Test
    void shouldReturn200WhenTicketExists() throws Exception {
        when(ticketService.findByCode("TCK-0001")).thenReturn(ticket("TCK-0001", TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/TCK-0001"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-0001"))
                .andExpect(jsonPath("$.eventName").value("Caribbean Music Fest 2026"));

        verify(ticketService).findByCode("TCK-0001");
    }

    @Test
    void shouldReturn404WhenTicketDoesNotExist() throws Exception {
        when(ticketService.findByCode("TCK-9999"))
                .thenThrow(new ResourceNotFoundException("Ticket not found: TCK-9999"));

        mockMvc.perform(get("/api/tickets/TCK-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found: TCK-9999"));
    }

    // TEST-CTRL-TKT-006 — tickets por usuario -> 200
    @Test
    void shouldReturn200WithTicketsOfUser() throws Exception {
        when(ticketService.findByUserEmail("andrea@email.com")).thenReturn(List.of(
                ticket("TCK-0001", TicketStatus.PAID),
                ticket("TCK-0002", TicketStatus.CANCELLED)));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-0001"))
                .andExpect(jsonPath("$[1].status").value("CANCELLED"));

        verify(ticketService).findByUserEmail("andrea@email.com");
    }

    // TEST-CTRL-TKT-007 — tickets PAID por evento -> 200
    @Test
    void shouldReturn200WithPaidTicketsOfEvent() throws Exception {
        when(ticketService.findPaidTicketsByEvent("CMF-2026")).thenReturn(List.of(
                ticket("TCK-0001", TicketStatus.PAID),
                ticket("TCK-0003", TicketStatus.PAID)));

        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[1].status").value("PAID"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }

    // TEST-CTRL-TKT-008 — cancelar válido -> 200 (AC-CTRL-011)
    @Test
    void shouldReturn200WhenCancellingPaidTicket() throws Exception {
        when(ticketService.cancel("TCK-0001")).thenReturn(ticket("TCK-0001", TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/TCK-0001/cancel"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-0001"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TCK-0001");
    }

    // TEST-CTRL-TKT-009 — cancelar inválido (ticket USED, AC-CTRL-012) -> 409
    @Test
    void shouldReturn409WhenCancellingUsedTicket() throws Exception {
        when(ticketService.cancel("TCK-0001"))
                .thenThrow(new BusinessRuleException("Only PAID tickets can be cancelled"));

        mockMvc.perform(patch("/api/tickets/TCK-0001/cancel"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Only PAID tickets can be cancelled"));
    }

    // TEST-CTRL-TKT-010 — usar válido -> 200 (AC-CTRL-013)
    @Test
    void shouldReturn200WhenMarkingPaidTicketAsUsed() throws Exception {
        when(ticketService.markAsUsed("TCK-0001")).thenReturn(ticket("TCK-0001", TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/TCK-0001/use"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TCK-0001");
    }

    // TEST-CTRL-TKT-011 — usar inválido (ticket CANCELLED, AC-CTRL-014) -> 409
    @Test
    void shouldReturn409WhenMarkingCancelledTicketAsUsed() throws Exception {
        when(ticketService.markAsUsed("TCK-0001"))
                .thenThrow(new BusinessRuleException("A cancelled ticket cannot be used"));

        mockMvc.perform(patch("/api/tickets/TCK-0001/use"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A cancelled ticket cannot be used"));
    }
}
