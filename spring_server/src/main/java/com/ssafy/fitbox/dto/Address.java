package com.ssafy.fitbox.dto;

public class Address {
    private int id;
    private Integer userId;
    private String address;
    private String zoneCode;
    private String roadAddress;
    private String detailAddress;

    public Address() {}

    public Address(int id, Integer userId, String address) {
        this.id = id;
        this.userId = userId;
        setAddress(address);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getAddress() {
        String road = roadAddress == null ? "" : roadAddress.trim();
        String zone = zoneCode == null ? "" : zoneCode.trim();
        String detail = detailAddress == null ? "" : detailAddress.trim();

        if (road.isBlank()) {
            return address == null ? "" : address;
        }

        String roadWithZone = zone.isBlank()
                ? road
                : "[" + zone.replace("[", "").replace("]", "") + "] " + road;

        return detail.isBlank() ? roadWithZone : roadWithZone + " " + detail;
    }

    public void setAddress(String address) {
        this.address = address;
        applyAddressFallback(address);
    }

    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }

    public String getRoadAddress() { return roadAddress; }
    public void setRoadAddress(String roadAddress) { this.roadAddress = roadAddress; }

    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }

    public void normalizeAddressFields() {
        if ((roadAddress == null || roadAddress.isBlank())
                && address != null
                && !address.isBlank()) {
            applyAddressFallback(address);
        }
        address = getAddress();
    }

    private void applyAddressFallback(String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (roadAddress != null && !roadAddress.isBlank()) {
            return;
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.startsWith("[") && normalized.contains("]")) {
            int endIndex = normalized.indexOf("]");
            zoneCode = normalized.substring(1, endIndex).trim();
            normalized = normalized.substring(endIndex + 1).trim();
        }

        roadAddress = normalized;
        detailAddress = "";
    }
}
