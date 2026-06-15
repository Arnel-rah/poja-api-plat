package api.poja.io.repository.jpa;

import api.poja.io.repository.model.Invoice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
  Optional<Invoice> findByUserIdAndId(String userId, String id);
}
