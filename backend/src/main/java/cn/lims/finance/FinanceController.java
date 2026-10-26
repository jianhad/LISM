package cn.lims.finance;

import cn.lims.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    public FinanceController(JdbcTemplate jdbc, AuditService audit){this.jdbc=jdbc;this.audit=audit;}

    @GetMapping("/price-lists")
    @PreAuthorize("hasAuthority('finance:charge:view')")
    public List<PriceRow> prices(){return jdbc.query("""
            select p.id,p.item_code,p.item_name,p.test_type,p.unit,p.unit_price,p.unit_cost,
                   p.valid_from,p.valid_to,p.status,p.version,
                   exists(select 1 from fin_charge_order_item i where i.price_item_id=p.id) referenced
            from fin_price_item p where p.deleted=false order by p.item_code
            """,(r,n)->new PriceRow(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getBigDecimal(6),r.getBigDecimal(7),r.getObject(8,LocalDate.class),r.getObject(9,LocalDate.class),r.getString(10),r.getInt(11),r.getBoolean(12)));}

    @PostMapping("/price-lists") @Transactional
    @PreAuthorize("hasAuthority('finance:price:manage')")
    public Id createPrice(@Valid @RequestBody PriceRequest q,Authentication a){Long id=jdbc.queryForObject("insert into fin_price_item(item_code,item_name,test_type,unit,unit_price,unit_cost,valid_from,valid_to,status) values(?,?,?,?,?,?,?,?,?) returning id",Long.class,q.itemCode(),q.itemName(),q.testType(),q.unit(),q.unitPrice(),q.unitCost(),q.validFrom(),q.validTo(),q.status());audit.record(a,"finance:price:create","PRICE",id,null,q.toString());return new Id(id);}

    @PutMapping("/price-lists/{id}") @Transactional
    @PreAuthorize("hasAuthority('finance:price:manage')")
    public void updatePrice(@PathVariable long id,@Valid @RequestBody PriceRequest q,Authentication a){ensurePriceNotReferenced(id,"该检测项目已被收费单引用，不能修改");int c=jdbc.update("update fin_price_item set item_code=?,item_name=?,test_type=?,unit=?,unit_price=?,unit_cost=?,valid_from=?,valid_to=?,status=?,updated_at=current_timestamp,version=version+1 where id=? and deleted=false and version=?",q.itemCode(),q.itemName(),q.testType(),q.unit(),q.unitPrice(),q.unitCost(),q.validFrom(),q.validTo(),q.status(),id,q.version());if(c==0)throw new IllegalArgumentException("价目不存在或已被修改");audit.record(a,"finance:price:update","PRICE",id,null,q.toString());}

    @DeleteMapping("/price-lists/{id}") @Transactional
    @PreAuthorize("hasAuthority('finance:price:manage')")
    public void deletePrice(@PathVariable long id,Authentication a){ensurePriceNotReferenced(id,"该检测项目已被收费单引用，不能删除");if(jdbc.update("update fin_price_item set deleted=true,status='DISABLED' where id=? and deleted=false",id)==0)throw new IllegalArgumentException("价目不存在");audit.record(a,"finance:price:delete","PRICE",id,null,"deleted");}

    @PostMapping("/charge-calculations")
    @PreAuthorize("hasAuthority('finance:charge:view')")
    public Calculation calculate(@Valid @RequestBody CalculationRequest q){BigDecimal total=BigDecimal.ZERO,cost=BigDecimal.ZERO;for(var i:q.items()){var p=jdbc.queryForMap("select unit_price,unit_cost from fin_price_item where id=? and deleted=false and status='ENABLED'",i.priceItemId());total=total.add(((BigDecimal)p.get("unit_price")).multiply(i.quantity()));cost=cost.add(((BigDecimal)p.get("unit_cost")).multiply(i.quantity()));}return new Calculation(total,cost,total.subtract(q.discountAmount()==null?BigDecimal.ZERO:q.discountAmount()));}

    @GetMapping("/charge-orders")
    @PreAuthorize("hasAuthority('finance:charge:view')")
    public List<ChargeRow> orders(){return jdbc.query("""
            select o.id,o.order_no,o.customer_name,
                   coalesce(string_agg(i.item_code, ', ' order by i.id),'') item_codes,
                   coalesce(string_agg(i.item_name, '、' order by i.id),'') item_names,
                   o.sample_count,o.total_amount,o.discount_amount,o.receivable_amount,o.paid_amount,
                   o.cost_amount,o.status,o.remark,o.created_at,o.version
            from fin_charge_order o
            left join fin_charge_order_item i on i.order_id=o.id
            where o.deleted=false
            group by o.id order by o.id desc
            """,(r,n)->chargeRow(r));}

    @GetMapping("/charge-orders/{id}/items")
    @PreAuthorize("hasAuthority('finance:charge:view')")
    public List<ChargeItemRow> orderItems(@PathVariable long id){return jdbc.query("select price_item_id,item_code,item_name,unit,quantity,unit_price,amount from fin_charge_order_item where order_id=? order by id",(r,n)->new ChargeItemRow(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getBigDecimal(5),r.getBigDecimal(6),r.getBigDecimal(7)),id);}

    @PostMapping("/charge-orders") @Transactional
    @PreAuthorize("hasAuthority('finance:charge:manage')")
    public Id createOrder(@Valid @RequestBody ChargeRequest q,Authentication a){Calculation calc=calculate(new CalculationRequest(q.items(),q.discountAmount()));String no="SF"+System.currentTimeMillis();Long id=jdbc.queryForObject("insert into fin_charge_order(order_no,customer_name,sample_count,total_amount,discount_amount,receivable_amount,cost_amount,status,remark,created_by) values(?,?,?,?,?,?,?,'DRAFT',?,?) returning id",Long.class,no,q.customerName(),q.sampleCount(),calc.totalAmount(),nz(q.discountAmount()),calc.receivableAmount(),calc.costAmount(),q.remark(),userId(a));for(var i:q.items()){var p=jdbc.queryForMap("select item_code,item_name,unit,unit_price,unit_cost from fin_price_item where id=? and deleted=false",i.priceItemId());BigDecimal amount=((BigDecimal)p.get("unit_price")).multiply(i.quantity()),cost=((BigDecimal)p.get("unit_cost")).multiply(i.quantity());jdbc.update("insert into fin_charge_order_item(order_id,price_item_id,item_code,item_name,unit,quantity,unit_price,unit_cost,amount,cost_amount) values(?,?,?,?,?,?,?,?,?,?)",id,i.priceItemId(),p.get("item_code"),p.get("item_name"),p.get("unit"),i.quantity(),p.get("unit_price"),p.get("unit_cost"),amount,cost);}audit.record(a,"finance:charge:create","CHARGE",id,null,no);return new Id(id);}

    @PutMapping("/charge-orders/{id}") @Transactional
    @PreAuthorize("hasAuthority('finance:charge:manage')")
    public void updateOrder(@PathVariable long id,@Valid @RequestBody ChargeRequest q,Authentication a){Calculation calc=calculate(new CalculationRequest(q.items(),q.discountAmount()));if(jdbc.update("update fin_charge_order set customer_name=?,sample_count=?,total_amount=?,discount_amount=?,receivable_amount=?,cost_amount=?,remark=?,updated_at=current_timestamp,version=version+1 where id=? and deleted=false and status='DRAFT' and version=?",q.customerName(),q.sampleCount(),calc.totalAmount(),nz(q.discountAmount()),calc.receivableAmount(),calc.costAmount(),q.remark(),id,q.version())==0)throw new IllegalArgumentException("仅待确认收费单可修改，或数据已被其他用户修改");jdbc.update("delete from fin_charge_order_item where order_id=?",id);for(var i:q.items()){var p=jdbc.queryForMap("select item_code,item_name,unit,unit_price,unit_cost from fin_price_item where id=? and deleted=false and status='ENABLED'",i.priceItemId());BigDecimal amount=((BigDecimal)p.get("unit_price")).multiply(i.quantity()),cost=((BigDecimal)p.get("unit_cost")).multiply(i.quantity());jdbc.update("insert into fin_charge_order_item(order_id,price_item_id,item_code,item_name,unit,quantity,unit_price,unit_cost,amount,cost_amount) values(?,?,?,?,?,?,?,?,?,?)",id,i.priceItemId(),p.get("item_code"),p.get("item_name"),p.get("unit"),i.quantity(),p.get("unit_price"),p.get("unit_cost"),amount,cost);}audit.record(a,"finance:charge:update","CHARGE",id,null,q.toString());}

    @DeleteMapping("/charge-orders/{id}") @Transactional
    @PreAuthorize("hasAuthority('finance:charge:manage')")
    public void deleteOrder(@PathVariable long id,Authentication a){if(jdbc.update("update fin_charge_order set deleted=true,status='VOID',updated_at=current_timestamp where id=? and deleted=false and status='DRAFT'",id)==0)throw new IllegalArgumentException("仅待确认收费单可删除");audit.record(a,"finance:charge:delete","CHARGE",id,"DRAFT","DELETED");}

    @PostMapping("/charge-orders/{id}/confirm") @Transactional
    @PreAuthorize("hasAuthority('finance:charge:manage')")
    public void confirm(@PathVariable long id,Authentication a){if(jdbc.update("update fin_charge_order set status='CONFIRMED',confirmed_by=?,confirmed_at=current_timestamp,updated_at=current_timestamp where id=? and status='DRAFT'",userId(a),id)==0)throw new IllegalArgumentException("仅草稿收费单可确认");audit.record(a,"finance:charge:confirm","CHARGE",id,"DRAFT","CONFIRMED");}

    @PostMapping("/charge-orders/{id}/return") @Transactional
    @PreAuthorize("hasAuthority('finance:charge:manage')")
    public void returnOrder(@PathVariable long id,Authentication a){if(jdbc.update("update fin_charge_order set status='DRAFT',confirmed_by=null,confirmed_at=null,updated_at=current_timestamp,version=version+1 where id=? and status='CONFIRMED' and paid_amount=0",id)==0)throw new IllegalArgumentException("仅未发生收款的待收款收费单可退回");audit.record(a,"finance:charge:return","CHARGE",id,"CONFIRMED","DRAFT");}

    @PostMapping("/charge-orders/{id}/void") @Transactional
    @PreAuthorize("hasAuthority('finance:charge:manage')")
    public void voidOrder(@PathVariable long id,Authentication a){if(jdbc.update("update fin_charge_order set status='VOID',updated_at=current_timestamp where id=? and status<>'VOID'",id)==0)throw new IllegalArgumentException("收费单不存在或已作废");audit.record(a,"finance:charge:void","CHARGE",id,null,"VOID");}

    @GetMapping("/charge-orders/{id}/payments")
    @PreAuthorize("hasAuthority('finance:charge:view')")
    public List<PaymentRow> payments(@PathVariable long id){return jdbc.query("select id,payment_no,amount,method,paid_at,reference_no,remark from fin_payment where order_id=? order by paid_at",(r,n)->new PaymentRow(r.getLong(1),r.getString(2),r.getBigDecimal(3),r.getString(4),r.getObject(5,LocalDateTime.class),r.getString(6),r.getString(7)),id);}

    @PostMapping("/charge-orders/{id}/payments") @Transactional
    @PreAuthorize("hasAuthority('finance:payment:manage')")
    public Id payment(@PathVariable long id,@Valid @RequestBody PaymentRequest q,Authentication a){var order=jdbc.queryForMap("select receivable_amount,paid_amount,status from fin_charge_order where id=? and deleted=false",id);if("DRAFT".equals(order.get("status"))||"VOID".equals(order.get("status")))throw new IllegalArgumentException("收费单未确认或已作废");BigDecimal paid=(BigDecimal)order.get("paid_amount"),due=(BigDecimal)order.get("receivable_amount"),next=paid.add(q.amount());if(next.compareTo(due)>0)throw new IllegalArgumentException("累计收款不能超过应收金额");String no="SK"+System.currentTimeMillis();Long pid=jdbc.queryForObject("insert into fin_payment(payment_no,order_id,amount,method,paid_at,reference_no,remark,received_by) values(?,?,?,?,?,?,?,?) returning id",Long.class,no,id,q.amount(),q.method(),q.paidAt()==null?LocalDateTime.now():q.paidAt(),q.referenceNo(),q.remark(),userId(a));jdbc.update("update fin_charge_order set paid_amount=?,status=?,updated_at=current_timestamp where id=?",next,next.compareTo(due)==0?"PAID":"PART_PAID",id);audit.record(a,"finance:payment:create","PAYMENT",pid,null,no);return new Id(pid);}

    @GetMapping("/receivables")
    @PreAuthorize("hasAuthority('finance:charge:view')")
    public List<ChargeRow> receivables(){return jdbc.query("""
            select o.id,o.order_no,o.customer_name,
                   coalesce(string_agg(i.item_code, ', ' order by i.id),'') item_codes,
                   coalesce(string_agg(i.item_name, '、' order by i.id),'') item_names,
                   o.sample_count,o.total_amount,o.discount_amount,o.receivable_amount,o.paid_amount,
                   o.cost_amount,o.status,o.remark,o.created_at,o.version
            from fin_charge_order o
            left join fin_charge_order_item i on i.order_id=o.id
            where o.deleted=false and o.status in('CONFIRMED','PART_PAID')
            group by o.id order by o.created_at
            """,(r,n)->chargeRow(r));}

    @GetMapping("/summary") @PreAuthorize("hasAuthority('finance:charge:view')")
    public Summary summary(){return jdbc.queryForObject("select coalesce(sum(receivable_amount),0),coalesce(sum(paid_amount),0),coalesce(sum(receivable_amount-paid_amount),0),count(*) filter(where status in('CONFIRMED','PART_PAID')) from fin_charge_order where deleted=false and status<>'VOID'",(r,n)->new Summary(r.getBigDecimal(1),r.getBigDecimal(2),r.getBigDecimal(3),r.getLong(4)));}

    private Long userId(Authentication a){return jdbc.queryForObject("select id from sys_user where username=? and deleted=false",Long.class,a.getName());}
    private static BigDecimal nz(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
    private void ensurePriceNotReferenced(long id,String message){Integer count=jdbc.queryForObject("select count(*) from fin_charge_order_item where price_item_id=?",Integer.class,id);if(count!=null&&count>0)throw new IllegalArgumentException(message);}
    private static ChargeRow chargeRow(java.sql.ResultSet r)throws java.sql.SQLException{return new ChargeRow(
            r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getInt(6),
            r.getBigDecimal(7),r.getBigDecimal(8),r.getBigDecimal(9),r.getBigDecimal(10),r.getBigDecimal(11),
            r.getString(12),r.getString(13),r.getObject(14,LocalDateTime.class),r.getInt(15));}
    public record Id(Long id){}
    public record PriceRow(long id,String itemCode,String itemName,String testType,String unit,BigDecimal unitPrice,BigDecimal unitCost,LocalDate validFrom,LocalDate validTo,String status,int version,boolean referenced){}
    public record PriceRequest(@NotBlank String itemCode,@NotBlank String itemName,@NotBlank String testType,@NotBlank String unit,@NotNull @Positive BigDecimal unitPrice,@NotNull BigDecimal unitCost,@NotNull LocalDate validFrom,LocalDate validTo,@NotBlank String status,int version){}
    public record CalculationItem(@NotNull Long priceItemId,@NotNull @Positive BigDecimal quantity){}
    public record CalculationRequest(@NotEmpty List<CalculationItem> items,BigDecimal discountAmount){}
    public record Calculation(BigDecimal totalAmount,BigDecimal costAmount,BigDecimal receivableAmount){}
    public record ChargeRequest(@NotBlank String customerName,int sampleCount,BigDecimal discountAmount,String remark,@NotEmpty List<CalculationItem> items,int version){}
    public record ChargeItemRow(long priceItemId,String itemCode,String itemName,String unit,BigDecimal quantity,BigDecimal unitPrice,BigDecimal amount){}
    public record ChargeRow(long id,String orderNo,String customerName,String itemCodes,String itemNames,int sampleCount,BigDecimal totalAmount,BigDecimal discountAmount,BigDecimal receivableAmount,BigDecimal paidAmount,BigDecimal costAmount,String status,String remark,LocalDateTime createdAt,int version){}
    public record PaymentRequest(@NotNull @Positive BigDecimal amount,@NotBlank String method,LocalDateTime paidAt,String referenceNo,String remark){}
    public record PaymentRow(long id,String paymentNo,BigDecimal amount,String method,LocalDateTime paidAt,String referenceNo,String remark){}
    public record Summary(BigDecimal receivable,BigDecimal paid,BigDecimal outstanding,long outstandingOrders){}
}
