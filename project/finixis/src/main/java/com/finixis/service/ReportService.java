package com.finixis.service;

import com.finixis.model.GeneratedFile;
import com.finixis.model.Invoice;
import com.finixis.model.Transaction;
import com.finixis.repository.ReportRepository;
import com.finixis.repository.impl.JdbcReportRepository;
import com.finixis.viewmodel.FileGenerationService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ReportService {

    private final ReportRepository repo;
    private static final AtomicInteger invoiceSeq = new AtomicInteger(1000);

    public ReportService() { this.repo = new JdbcReportRepository(); }

    public List<GeneratedFile> getAll() { return repo.findAll(); }

    /** Generate a Report PDF+Excel from live DB transactions, persist records, return them. */
    public List<GeneratedFile> generateReport(TransactionService txnService) {
        List<Transaction> txns = txnService.getAll();
        try {
            List<GeneratedFile> files = FileGenerationService.generateReport(txns);
            files.forEach(repo::save);
            return files;
        } catch (Exception e) { throw new RuntimeException("Report generation failed", e); }
    }

    /**
     * Generate an Invoice PDF+Excel.
     * Credit transactions are converted to Invoice objects for the generator.
     */
    public List<GeneratedFile> createInvoice(TransactionService txnService) {
        List<Transaction> credits = txnService.getAllCredits();
        List<Invoice> invoices = new ArrayList<>();
        for (Transaction t : credits) {
            int num = invoiceSeq.getAndIncrement();
            Invoice inv = new Invoice(t.getId(), "INV-" + num,
                    t.getCustomerId(), t.getCustomerName(),
                    t.getDate(), t.getDate().plusDays(30),
                    t.getAmount(), t.getAmount() * 0.18, t.getAmount() * 1.18,
                    List.of(new Invoice.LineItem(
                            t.getDescription() != null ? t.getDescription() : "Services",
                            1, t.getAmount(), t.getAmount())));
            invoices.add(inv);
        }
        try {
            List<GeneratedFile> files = FileGenerationService.createInvoice(invoices);
            files.forEach(repo::save);
            return files;
        } catch (Exception e) { throw new RuntimeException("Invoice generation failed", e); }
    }

    /** Export all transactions to PDF+Excel, persist records, return them. */
    public List<GeneratedFile> exportTransactions(TransactionService txnService) {
        List<Transaction> txns = txnService.getAll();
        try {
            List<GeneratedFile> files = FileGenerationService.exportTransactions(txns);
            files.forEach(repo::save);
            return files;
        } catch (Exception e) { throw new RuntimeException("Export failed", e); }
    }
}
