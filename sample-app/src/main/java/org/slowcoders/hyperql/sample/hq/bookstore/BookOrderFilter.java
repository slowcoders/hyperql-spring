package org.slowcoders.hyperql.sample.hq.bookstore;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperql.sample.hq.bookstore.model.BookOrder;
import org.slowcoders.hyperquery.core.QFilter;

import java.net.HttpURLConnection;

@Getter
@Setter

public class BookOrderFilter extends QFilter<BookOrder> {
    private Long bookId;


//       @Override
//       public void setHpmsOwsWebService(HttpServletRequest request, HttpServletResponse response, OxiDocuCollectVO oxiDocuCollectVO) throws Exception {
//           log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> Ows(PMS) Saas Trans Start >>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//
//           if (oxiDocuCollectVO.getRequestDocumentXmlContent().trim().length() == 0) {
//               this.getErrorResult(response, "Failed to read request body");
//               return;
//           }
//
//           this.setDocumentCollect(oxiDocuCollectVO);
//
//           String sOwsUrl = IfHpmsXmlUrl + owsURl + request.getRequestURI().replace("hpms/","");
//
//           log.debug(">> Ows(PMS) Saas Url : " + sOwsUrl);
//
//           // 요청을 보낼 URL
//           URL url = new URL(sOwsUrl);
//           HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
//
//           try {
//
//               // Connection Timeout 설정 (밀리초 단위)
//               httpConn.setConnectTimeout(5000);  // 5초
//
//               // Read Timeout 설정 (밀리초 단위)
//               httpConn.setReadTimeout(360000);    // 360초, 6분
//
//               // HTTP 메소드 설정
//               httpConn.setRequestMethod(request.getMethod());
//
//               String sRequestHeader = "";
//
//               Enumeration eHeader = request.getHeaderNames();
//               while (eHeader.hasMoreElements()) {
//                   String sReqName = (String) eHeader.nextElement();
//                   String sReqValue = request.getHeader(sReqName);
//
//                   if (sReqName.toUpperCase().equals("SOAPACTION")) {
//                       sReqValue = sReqValue.replaceAll("\"", "");
//                   }
//
//                   httpConn.setRequestProperty(sReqName, sReqValue);
//               }
//
//               if (proxyNo.indexOf("dev_") < 0) {
//                   if (owsSaasToken == null) {
//                       this.getToken(owsDocuDir);        // ows token
//                   }
//
//                   httpConn.setRequestProperty("Authorization", "Bearer " + owsSaasToken);
//                   httpConn.setRequestProperty("chainId", owsChainId);
//                   httpConn.setRequestProperty("propertyId", owsPropertyId);
//                   httpConn.setRequestProperty("userId", owsUserId);
//               }
//
//               httpConn.setRequestProperty("User-Agent", owsDocuDir);
//
//               httpConn.setDoOutput(true);
//
//               String sBodyStr = oxiDocuCollectVO.getRequestDocumentXmlContent().replaceAll("<hc:", "<");
//               sBodyStr = sBodyStr.replaceAll("</hc:", "</");
//               sBodyStr = sBodyStr.replaceAll("<c:PhoneNumber>", "<PhoneNumber xmlns=\"http://webservices.micros.com/og/4.3/Common/\">");
//               sBodyStr = sBodyStr.replaceAll("<c:lastName>", "<lastName xmlns=\"http://webservices.micros.com/og/4.3/Common/\">");
//               sBodyStr = sBodyStr.replaceAll("<c:nameTitle>", "<nameTitle xmlns=\"http://webservices.micros.com/og/4.3/Common/\">");
//               sBodyStr = sBodyStr.replaceAll("<c:firstName>", "<firstName xmlns=\"http://webservices.micros.com/og/4.3/Common/\">");
//               sBodyStr = sBodyStr.replaceAll("<c:", "<").replaceAll("</c:", "</");
//               sBodyStr = sBodyStr.replaceAll("<m:", "<").replaceAll("</m:", "</");
//               sBodyStr = sBodyStr.replaceAll("<SOAP-ENV:", "<soap:").replaceAll("</SOAP-ENV:", "</soap:");
//               sBodyStr = sBodyStr.replaceAll("<s0:", "<").replaceAll("</s0:", "</");
//               sBodyStr = sBodyStr.replaceAll("<s2:", "<").replaceAll("</s2:", "</");
//               sBodyStr = sBodyStr.replaceAll("<s4:", "<").replaceAll("</s4:", "</");
//               sBodyStr = sBodyStr.replaceAll("<s6:", "<").replaceAll("</s6:", "</");
//               sBodyStr = sBodyStr.replaceAll("<s7:", "<").replaceAll("</s7:", "</");
//               sBodyStr = sBodyStr.replaceAll("xmlns:tns=\"http://HTName/\"", "");
//               sBodyStr = sBodyStr.replaceAll("xmlns:soapenv", "xmlns:soap");
//               sBodyStr = sBodyStr.replaceAll("xmlns:s4", "xmlns");
//               sBodyStr = sBodyStr.replaceAll("<FetchHouseKeepingRoomStatusRequest>", "<FetchHouseKeepingRoomStatusRequest xmlns=\"http://webservices.micros.com/ows/5.1/HouseKeeping.wsdl\">");
//               sBodyStr = sBodyStr.replaceAll("<UpdateHouseKeepingRoomStatusRequest>", "<UpdateHouseKeepingRoomStatusRequest xmlns=\"http://webservices.micros.com/ows/5.1/HouseKeeping.wsdl\">");
//
//               try (OutputStream osReq = httpConn.getOutputStream()) {
//                   byte[] input = sBodyStr.getBytes("utf-8");
//                   osReq.write(input, 0, input.length);
//
//                   log.debug(">> Ows(PMS) Saas Trans Send Body=========" + osReq.toString());
//               }
//
//               httpConn.setConnectTimeout(httpTimeOut);
//               httpConn.setReadTimeout(httpTimeOut);
//
//               // 응답 코드 확인
//               int responseCode = httpConn.getResponseCode();
//               log.debug(">> Ows(PMS) Saas Trans Response Code: " + responseCode);
//
//               response.setStatus(responseCode);
//
//               // 응답 헤더의 정보를 모두 출력
//               String sRetHeader = "";
//               for (Map.Entry<String, List<String>> header : httpConn.getHeaderFields().entrySet()) {
//                   for (String value : header.getValue()) {
//                       response.setHeader(header.getKey(), value);
//
//                       sRetHeader += sRetHeader.equals("")?"":", " + header.getKey() + "=" + value;
//                   }
//               }
//
//               log.debug(">> Ows(PMS) Saas Trans Response Header : " + sRetHeader);
//
//               OutputStream outputStream = response.getOutputStream();
//               ByteArrayOutputStream baos = new ByteArrayOutputStream();
//
//               // 응답 내용(BODY) 구하기
//               int length;
//               byte[] buf = new byte[2048];
//               try {
//                   while ((length = httpConn.getInputStream().read(buf, 0, buf.length)) != -1) {
//                       outputStream.write(buf, 0, length);
//                       baos.write(buf, 0, length);
//                   }
//                   log.error(">> Ows(PMS) Saas Trans Response Body : " + baos.toString());
//               } catch (Exception e) {
//                   log.error(">> Ows(PMS) Saas Trans Response Body Error : " + e.getMessage());
//               } finally {
//                   outputStream.close();
//               }
//
//               if (byPassYn.equals("N")) {
//                   oxiDocuCollectVO.setResponseDocumentHeaderContent(sRetHeader);
//                   oxiDocuCollectVO.setResponseDocumentXmlContent(baos.toString());
//
//                   this.setDocumentCollect(oxiDocuCollectVO);
//               }
//
//           } catch (Exception e) {
//               log.error(">> Ows(PMS) Saas Trans Error : " + e.getMessage());
//               //log.error("",e);
//           } finally {
//               httpConn.disconnect();
//           }
//
//           log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> Ows(PMS) Saas Trans End >>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//       }
}
