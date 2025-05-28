package tests;

import core.APIBase;
import core.AuthHandler;
import core.Main;
import core.RetryHandler;
import core.TestResultHolder;
import model.APIInfo;
import utils.EmailSender;

import java.util.*;
import java.util.stream.Collectors;


public class BackToNormalTry {
	
	 public static String allureReportUrl = "https://saburumman.github.io/api-framework/index.html";

	    public static void main(String[] args) {
	        try {
	            System.out.println("⏳ Waiting 10 minutes before final retry...");
	            Thread.sleep(600_000); // 10 minutes

	            List<Map<String, Object>> failedResults = TestResultHolder.getAllResults().stream()
	                .filter(res -> {
	                    Object code = res.get("status_code");
	                    return !(code instanceof Integer) || (Integer) code != 200;
	                })
	                .collect(Collectors.toList());

	            if (failedResults.isEmpty()) {
	                System.out.println("✅ No failed APIs to retry.");
	                return;
	            }

	            List<APIInfo> failedApis = failedResults.stream()
	                .map(Main::mapToApiInfo)
	                .collect(Collectors.toList());

	            APIBase apiBase = Main.initializeApiContextFromLatest(); // You’ll define this
	            RetryHandler finalRetryHandler = new RetryHandler();
	            List<Map<String, Object>> retryResults = Main.retryFailedApis(apiBase, failedApis, finalRetryHandler);

	            List<APIInfo> backToNormal = new ArrayList<>();
	            List<APIInfo> stillFailing = new ArrayList<>();

	            for (APIInfo api : failedApis) {
	                if (!finalRetryHandler.getFailedAPIs().contains(api)) {
	                    backToNormal.add(api);
	                } else {
	                    stillFailing.add(api);
	                }
	            }

	            if (!backToNormal.isEmpty()) {
	                EmailSender.sendBackToNormalSuccess(backToNormal, allureReportUrl);
	            }

	            if (!stillFailing.isEmpty()) {
	                EmailSender.sendStillFailed(Main.toApiInfoMaps(stillFailing), allureReportUrl);
	            }

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}