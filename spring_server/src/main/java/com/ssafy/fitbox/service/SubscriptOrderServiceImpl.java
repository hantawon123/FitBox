package com.ssafy.fitbox.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.fitbox.dto.SubscriptOrder;
import com.ssafy.fitbox.dto.SubscriptionGroup;
import com.ssafy.fitbox.dto.SubscriptionTemplate;
import com.ssafy.fitbox.dto.request.SelectedDateSubscriptOrderRequest;
import com.ssafy.fitbox.dto.request.SubscriptionCreateRequest;
import com.ssafy.fitbox.dto.request.SubscriptionDateMealRequest;
import com.ssafy.fitbox.dto.request.SubscriptionMultiDateOrderRequest;
import com.ssafy.fitbox.dto.request.SubscriptionTemplateRequest;
import com.ssafy.fitbox.dto.response.SubscriptionResponse;
import com.ssafy.fitbox.dto.response.SubscriptionTemplateResponse;
import com.ssafy.fitbox.mapper.StoreDailyCapacityMapper;
import com.ssafy.fitbox.mapper.SubscriptOrderMapper;
import com.ssafy.fitbox.mapper.SubscriptionGroupMapper;
import com.ssafy.fitbox.mapper.SubscriptionTemplateMapper;

@Service
public class SubscriptOrderServiceImpl implements SubscriptOrderService {

    private static final String RECEIVE_TYPE_DELIVERY = "DELIVERY";

    private static final String SUBSCRIPTION_STATUS_ACTIVE = "ACTIVE";
    private static final String SUBSCRIPTION_STATUS_ONE_TIME = "ONE_TIME";

    private final SubscriptOrderMapper subscriptOrderMapper;
    private final SubscriptionGroupMapper subscriptionGroupMapper;
    private final SubscriptionTemplateMapper subscriptionTemplateMapper;
    private final StoreDailyCapacityMapper storeDailyCapacityMapper;

    public SubscriptOrderServiceImpl(
            SubscriptOrderMapper subscriptOrderMapper,
            SubscriptionGroupMapper subscriptionGroupMapper,
            SubscriptionTemplateMapper subscriptionTemplateMapper,
            StoreDailyCapacityMapper storeDailyCapacityMapper
    ) {
        this.subscriptOrderMapper = subscriptOrderMapper;
        this.subscriptionGroupMapper = subscriptionGroupMapper;
        this.subscriptionTemplateMapper = subscriptionTemplateMapper;
        this.storeDailyCapacityMapper = storeDailyCapacityMapper;
    }

    @Override
    public ArrayList<SubscriptOrder> selectAll() {
        return subscriptOrderMapper.selectAll();
    }

    @Override
    public SubscriptOrder selectById(Long id) {
        return subscriptOrderMapper.selectById(id);
    }

    @Override
    public int insert(SubscriptOrder subscriptOrder) {
        if (subscriptOrder.getQuantity() == null || subscriptOrder.getQuantity() <= 0) {
            subscriptOrder.setQuantity(1);
        }

        return subscriptOrderMapper.insert(subscriptOrder);
    }

    @Override
    public int update(SubscriptOrder subscriptOrder) {
        if (subscriptOrder.getQuantity() == null || subscriptOrder.getQuantity() <= 0) {
            subscriptOrder.setQuantity(1);
        }

        return subscriptOrderMapper.update(subscriptOrder);
    }

    @Override
    public int delete(Long id) {
        return subscriptOrderMapper.delete(id);
    }

    @Override
    @Transactional
    public List<SubscriptOrder> insertSelectedDateOrders(
            SelectedDateSubscriptOrderRequest request
    ) {
        validateSelectedDateRequest(request);

        SubscriptionMultiDateOrderRequest multiRequest = new SubscriptionMultiDateOrderRequest();
        multiRequest.setUserId(request.getUserId());
        multiRequest.setReceiveType(request.getReceiveType());
        multiRequest.setStoreId(request.getStoreId());
        multiRequest.setAddress(request.getAddress());

        List<SubscriptionDateMealRequest> items = request.getReceiveDates()
                .stream()
                .distinct()
                .sorted()
                .map(receiveDate -> {
                    SubscriptionDateMealRequest item = new SubscriptionDateMealRequest();
                    item.setReceiveDate(receiveDate);
                    item.setMealId(request.getMealId());
                    item.setQuantity(1);
                    return item;
                })
                .toList();

        multiRequest.setItems(items);

        return insertMultiDateOrders(multiRequest);
    }

    @Override
    @Transactional
    public List<SubscriptOrder> insertMultiDateOrders(
            SubscriptionMultiDateOrderRequest request
    ) {
        validateMultiDateOrderRequest(request);

        LocalDate minDate = request.getItems()
                .stream()
                .map(SubscriptionDateMealRequest::getReceiveDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        LocalDate maxDate = request.getItems()
                .stream()
                .map(SubscriptionDateMealRequest::getReceiveDate)
                .max(LocalDate::compareTo)
                .orElse(minDate);

        SubscriptionGroup group = new SubscriptionGroup();
        group.setUserId(request.getUserId());
        group.setSubscriptionStartDate(minDate);
        group.setSubscriptionEndDate(maxDate);
        group.setNextOrderMonth(maxDate.plusMonths(1));
        group.setStatus(SUBSCRIPTION_STATUS_ONE_TIME);
        group.setReceiveType(request.getReceiveType());
        group.setStoreId(null);
        group.setAddress(getAddressIfDelivery(request));

        int groupInsertResult = subscriptionGroupMapper.insert(group);

        if (groupInsertResult <= 0 || group.getId() == null) {
            throw new IllegalStateException("구독 주문 그룹 생성에 실패했습니다.");
        }

        List<SubscriptOrder> createdOrders = new ArrayList<>();

        for (SubscriptionDateMealRequest item : request.getItems()) {
            int quantity = normalizeQuantity(item.getQuantity());

            SubscriptOrder order = createOrderForItem(
                    group.getId(),
                    request.getUserId(),
                    request.getReceiveType(),
                    null,
                    getAddressIfDelivery(request),
                    item.getReceiveDate(),
                    item.getMealId(),
                    quantity
            );

            int insertResult = subscriptOrderMapper.insert(order);

            if (insertResult <= 0) {
                throw new IllegalStateException("구독 주문 저장에 실패했습니다.");
            }

            createdOrders.add(order);
        }

        return createdOrders;
    }

    @Override
    @Transactional
    public SubscriptionResponse createMonthlySubscription(
            SubscriptionCreateRequest request
    ) {
        validateSubscriptionCreateRequest(request);

        LocalDate startDate = request.getSubscriptionStartDate();
        LocalDate firstCycleEndDate = startDate.plusMonths(1).minusDays(1);
        LocalDate nextOrderMonth = startDate.plusMonths(1);

        SubscriptionGroup group = new SubscriptionGroup();
        group.setUserId(request.getUserId());
        group.setSubscriptionStartDate(startDate);
        group.setSubscriptionEndDate(null);
        group.setNextOrderMonth(nextOrderMonth);
        group.setStatus(SUBSCRIPTION_STATUS_ACTIVE);
        group.setReceiveType(request.getReceiveType());
        group.setStoreId(null);
        group.setAddress(getAddressIfDelivery(request));

        int groupInsertResult = subscriptionGroupMapper.insert(group);

        if (groupInsertResult <= 0 || group.getId() == null) {
            throw new IllegalStateException("정기 구독 생성에 실패했습니다.");
        }

        for (SubscriptionTemplateRequest templateRequest : request.getTemplates()) {
            SubscriptionTemplate template = new SubscriptionTemplate();
            template.setSubscriptionGroupId(group.getId());
            template.setWeekOfMonth(templateRequest.getWeekOfMonth());
            template.setDayOfWeek(templateRequest.getDayOfWeek());
            template.setMealId(templateRequest.getMealId());
            template.setQuantity(normalizeQuantity(templateRequest.getQuantity()));

            int templateInsertResult = subscriptionTemplateMapper.insert(template);

            if (templateInsertResult <= 0) {
                throw new IllegalStateException("구독 식단표 저장에 실패했습니다.");
            }
        }

        List<SubscriptOrder> firstCycleOrders = createFirstCycleOrdersFromTemplates(
                group.getId(),
                request,
                startDate,
                firstCycleEndDate
        );

        for (SubscriptOrder order : firstCycleOrders) {
            int insertResult = subscriptOrderMapper.insert(order);

            if (insertResult <= 0) {
                throw new IllegalStateException("첫 달 구독 주문 생성에 실패했습니다.");
            }
        }

        return buildSubscriptionResponse(group.getId());
    }

    @Override
    public List<SubscriptionResponse> getUserSubscriptions(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId가 필요합니다.");
        }

        List<SubscriptionResponse> subscriptions =
                subscriptionGroupMapper.findSubscriptionsByUserId(userId);

        for (SubscriptionResponse subscription : subscriptions) {
            List<SubscriptionTemplateResponse> templates =
                    subscriptionTemplateMapper.findTemplateResponsesBySubscriptionGroupId(
                            subscription.getSubscriptionGroupId()
                    );

            subscription.setTemplates(templates);
        }

        return subscriptions;
    }

    @Override
    @Transactional
    public void cancelSubscription(Long subscriptionGroupId, Integer userId) {
        if (subscriptionGroupId == null || userId == null) {
            throw new IllegalArgumentException("구독 번호와 사용자 정보가 필요합니다.");
        }

        int updated = subscriptionGroupMapper.cancelSubscription(
                subscriptionGroupId,
                userId
        );

        if (updated == 0) {
            throw new IllegalArgumentException("취소할 수 있는 구독을 찾지 못했습니다.");
        }
    }

    private List<SubscriptOrder> createFirstCycleOrdersFromTemplates(
            Long subscriptionGroupId,
            SubscriptionCreateRequest request,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<SubscriptOrder> orders = new ArrayList<>();

        for (SubscriptionTemplateRequest template : request.getTemplates()) {
            LocalDate receiveDate = findDateByRelativeWeekAndDayOfWeek(
                    startDate,
                    template.getWeekOfMonth(),
                    template.getDayOfWeek()
            );

            if (receiveDate.isAfter(endDate)) {
                continue;
            }

            SubscriptOrder order = createOrderForItem(
                    subscriptionGroupId,
                    request.getUserId(),
                    request.getReceiveType(),
                    null,
                    getAddressIfDelivery(request),
                    receiveDate,
                    template.getMealId(),
                    normalizeQuantity(template.getQuantity())
            );

            orders.add(order);
        }

        return orders;
    }

    private LocalDate findDateByRelativeWeekAndDayOfWeek(
            LocalDate startDate,
            Integer relativeWeek,
            Integer dayOfWeek
    ) {
        DayOfWeek targetDayOfWeek = convertToJavaDayOfWeek(dayOfWeek);
        LocalDate weekStart = startDate.plusWeeks(relativeWeek - 1L);
        int daysUntilTarget = Math.floorMod(
                targetDayOfWeek.getValue() - weekStart.getDayOfWeek().getValue(),
                7
        );
        return weekStart.plusDays(daysUntilTarget);
    }

    private DayOfWeek convertToJavaDayOfWeek(Integer dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> DayOfWeek.SUNDAY;
            case 2 -> DayOfWeek.MONDAY;
            case 3 -> DayOfWeek.TUESDAY;
            case 4 -> DayOfWeek.WEDNESDAY;
            case 5 -> DayOfWeek.THURSDAY;
            case 6 -> DayOfWeek.FRIDAY;
            case 7 -> DayOfWeek.SATURDAY;
            default -> throw new IllegalArgumentException("dayOfWeek는 1~7이어야 합니다.");
        };
    }

    private SubscriptionResponse buildSubscriptionResponse(Long subscriptionGroupId) {
        SubscriptionGroup group = subscriptionGroupMapper.selectById(subscriptionGroupId);

        if (group == null) {
            throw new IllegalStateException("생성된 구독 정보를 찾을 수 없습니다.");
        }

        SubscriptionResponse response = new SubscriptionResponse();
        response.setSubscriptionGroupId(group.getId());
        response.setUserId(group.getUserId());
        response.setOrderTime(group.getOrderTime());
        response.setSubscriptionStartDate(group.getSubscriptionStartDate());
        response.setSubscriptionEndDate(group.getSubscriptionEndDate());
        response.setNextOrderMonth(group.getNextOrderMonth());
        response.setStatus(group.getStatus());
        response.setReceiveType(group.getReceiveType());
        response.setStoreId(group.getStoreId());
        response.setAddress(group.getAddress());

        List<SubscriptionTemplateResponse> templates =
                subscriptionTemplateMapper.findTemplateResponsesBySubscriptionGroupId(
                        group.getId()
                );

        response.setTemplates(templates);

        return response;
    }

    private void validateSubscriptionCreateRequest(
            SubscriptionCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("구독 정보가 없습니다.");
        }

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId가 필요합니다.");
        }

        if (request.getSubscriptionStartDate() == null) {
            throw new IllegalArgumentException("구독 시작일이 필요합니다.");
        }

        validateReceiveInfo(
                request.getReceiveType(),
                request.getStoreId(),
                request.getAddress()
        );

        if (request.getTemplates() == null || request.getTemplates().isEmpty()) {
            throw new IllegalArgumentException("구독 식단표를 하나 이상 추가해야 합니다.");
        }

        for (SubscriptionTemplateRequest template : request.getTemplates()) {
            validateTemplateRequest(template);
        }
    }

    private void validateTemplateRequest(
            SubscriptionTemplateRequest template
    ) {
        if (template == null) {
            throw new IllegalArgumentException("구독 식단표 정보가 없습니다.");
        }

        if (template.getWeekOfMonth() == null
                || template.getWeekOfMonth() < 1
                || template.getWeekOfMonth() > 5) {
            throw new IllegalArgumentException("weekOfMonth는 1~5 사이여야 합니다.");
        }

        if (template.getDayOfWeek() == null
                || template.getDayOfWeek() < 1
                || template.getDayOfWeek() > 7) {
            throw new IllegalArgumentException("dayOfWeek는 1~7 사이여야 합니다.");
        }

        if (template.getMealId() == null) {
            throw new IllegalArgumentException("mealId가 필요합니다.");
        }

        if (template.getQuantity() != null && template.getQuantity() <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }
    }

    private void validateSelectedDateRequest(
            SelectedDateSubscriptOrderRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("구독 주문 정보가 없습니다.");
        }

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId가 필요합니다.");
        }

        if (request.getMealId() == null) {
            throw new IllegalArgumentException("mealId가 필요합니다.");
        }

        if (request.getReceiveDates() == null || request.getReceiveDates().isEmpty()) {
            throw new IllegalArgumentException("수령 날짜를 하나 이상 선택해야 합니다.");
        }

        validateReceiveInfo(
                request.getReceiveType(),
                request.getStoreId(),
                request.getAddress()
        );
    }

    private void validateMultiDateOrderRequest(
            SubscriptionMultiDateOrderRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("구독 주문 정보가 없습니다.");
        }

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId가 필요합니다.");
        }

        validateReceiveInfo(
                request.getReceiveType(),
                request.getStoreId(),
                request.getAddress()
        );

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("구독 식단을 하나 이상 선택해야 합니다.");
        }

        for (SubscriptionDateMealRequest item : request.getItems()) {
            if (item.getReceiveDate() == null) {
                throw new IllegalArgumentException("수령 날짜가 필요합니다.");
            }

            if (item.getMealId() == null) {
                throw new IllegalArgumentException("mealId가 필요합니다.");
            }

            if (item.getQuantity() != null && item.getQuantity() <= 0) {
                throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
            }
        }
    }

    private void validateReceiveInfo(
            String receiveType,
            Long storeId,
            String address
    ) {
        if (receiveType == null || receiveType.isBlank()) {
            throw new IllegalArgumentException("receiveType이 필요합니다.");
        }

        if (!RECEIVE_TYPE_DELIVERY.equals(receiveType)) {
            throw new IllegalArgumentException("구독은 DELIVERY만 지원합니다.");
        }

        if (RECEIVE_TYPE_DELIVERY.equals(receiveType)
                && (address == null || address.isBlank())) {
            throw new IllegalArgumentException("배달 구독은 address가 필요합니다.");
        }
    }

    private void decreaseStoreCapacityForItems(
            Long storeId,
            List<SubscriptionDateMealRequest> items
    ) {
        for (SubscriptionDateMealRequest item : items) {
            int quantity = normalizeQuantity(item.getQuantity());

            int updatedCount = storeDailyCapacityMapper.decreaseRemainCount(
                    storeId,
                    item.getReceiveDate(),
                    quantity
            );

            if (updatedCount <= 0) {
                throw new IllegalArgumentException(
                        item.getReceiveDate() + " 날짜의 픽업 가능 수량이 부족합니다."
                );
            }
        }
    }

    private SubscriptOrder createOrderForItem(
            Long subscriptionGroupId,
            Integer userId,
            String receiveType,
            Long storeId,
            String address,
            LocalDate receiveDate,
            Long mealId,
            int quantity
    ) {
        SubscriptOrder order = new SubscriptOrder();

        order.setSubscriptionGroupId(subscriptionGroupId);
        order.setUserId(userId);
        order.setDateStart(receiveDate);
        order.setDateEnd(receiveDate);
        order.setQuantity(quantity);
        order.setReceiveType(receiveType);
        order.setStoreId(storeId);
        order.setAddress(address);

        setMealIdToMatchedWeekDay(
                order,
                receiveDate.getDayOfWeek(),
                mealId
        );

        return order;
    }

    private void setMealIdToMatchedWeekDay(
            SubscriptOrder order,
            DayOfWeek dayOfWeek,
            Long mealId
    ) {
        switch (dayOfWeek) {
            case MONDAY -> order.setMon(mealId);
            case TUESDAY -> order.setTue(mealId);
            case WEDNESDAY -> order.setWed(mealId);
            case THURSDAY -> order.setThu(mealId);
            case FRIDAY -> order.setFri(mealId);
            case SATURDAY -> order.setSat(mealId);
            case SUNDAY -> order.setSun(mealId);
        }
    }

    private Long getMealIdFromOrder(SubscriptOrder order) {
        if (order.getMon() != null) return order.getMon();
        if (order.getTue() != null) return order.getTue();
        if (order.getWed() != null) return order.getWed();
        if (order.getThu() != null) return order.getThu();
        if (order.getFri() != null) return order.getFri();
        if (order.getSat() != null) return order.getSat();
        return order.getSun();
    }

    private int normalizeQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return 1;
        }

        return quantity;
    }

    private String getAddressIfDelivery(SubscriptionMultiDateOrderRequest request) {
        if (!RECEIVE_TYPE_DELIVERY.equals(request.getReceiveType())) {
            return null;
        }

        return request.getAddress();
    }

    private String getAddressIfDelivery(SubscriptionCreateRequest request) {
        if (!RECEIVE_TYPE_DELIVERY.equals(request.getReceiveType())) {
            return null;
        }

        return request.getAddress();
    }
}
