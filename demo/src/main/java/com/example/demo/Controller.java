package com.example.demo;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.PAP.PAPTest;
import com.example.demo.PAP.PolicyStore;
import com.example.demo.PDP.PDP;
import com.example.demo.PEP.PEP;
import com.example.demo.PIP.PIPTest;
import com.example.demo.PIP.TrustScoreStore;
import com.example.demo.models.AccessRequest;
import com.example.demo.models.AuthRequest;
import com.example.demo.models.AuthRequestTango;
import com.example.demo.models.SimpleAccessRight;
import com.example.demo.models.CapabilityToken;
import com.example.demo.requester.Requester;
import com.google.gson.Gson;

@RestController
@RequestMapping("/api")
public class Controller {

    private final String pipConfig;
    private final String papConfig;
    private final String wallet;
    
    PIPTest pip;
    PAPTest pap;
    PDP pdp;
    PEP pep;
    Gson gson;

    @Autowired
    public Controller(Environment env) {
        this.pipConfig = System.getProperty("pipConfig");
        this.papConfig = System.getProperty("papConfig");
        this.wallet=System.getProperty("wallet");
        		
      //Create the PAP,PIP according to the args
        if(pipConfig.equals("test") && papConfig.equals("test") && wallet.equals("test")) {
    	
    	TrustScoreStore trustScores=new TrustScoreStore();
    	pip=new PIPTest(trustScores);
    	
    	PolicyStore policies=new PolicyStore();
    	pap=new PAPTest(policies);

    	pdp=new PDP(pip,pap,wallet);
    	
    	pep= new PEP(pdp);

    	gson=new Gson();
        } //else if(pipConfig.equals("erathostenes") && papConfig.equals("erathostenes") && wallet.equals("erathostenes")){}
        
        //erathostenes option 
        
        
        else {
        	System.err.println("Not a valid configuration."); System.exit(0);
        }
    }
	
    @PostMapping("/request-access")
    public String requestAccess(@RequestBody AuthRequest request) {
    	
    	//Create a requester with request data
    	Requester requester =new Requester(request.getDidSP(),request.getDidRequester(),request.getVerifiablePresentation());
    	
    	//Create access request
		String req=requester.requestAccess(request.getSar().getResource(), request.getSar().getAction());
		pep.parseRequest(req);
		
		//Process access request to obtain a Capability Token 
    	CapabilityToken ct=process(req);
    	String token = gson.toJson(ct);
    	if(ct==null) {
    		System.out.println("Capability Token couldn't be issued, please revise the request and try again.\n");
    		return "Capability Token couldn't be issued, please revise the request and try again.\n";
    	}
    	System.out.println("Capability Token successfully issued.\n");
    	return token ;
    }
    
    public CapabilityToken process(String req) {
    	
    	//Send request to PEP for issuing the Capability token
    	String requestJson=gson.toJson(req);
    	CapabilityToken ct=pep.sendRequest(requestJson);
    	
    	return ct;
    }
    
    @PostMapping("/access-token-tango")
    public String accessTokenForTango(@RequestBody AuthRequestTango request) {
    	
    	//Create a requester with request data -> change the token tango instead VP
    	Requester requester =new Requester(request.getDidSP(),request.getDidRequester(),request.getToken());
    	
    	//Create access request
		String req=requester.requestAccessToken(request.getSar().getResource(), request.getSar().getAction(),request.getToken());
		//pep.parseRequest(req);
		
		//Process access request to obtain a Capability Token 
		System.out.println("token received "+req);
    	CapabilityToken ct=processTokenTango(req);
    	String token = gson.toJson(ct);
    	if(ct==null) {
    		System.out.println("Capability Token couldn't be issued, please revise the request and try again.\n");
    		return "Capability Token couldn't be issued, please revise the request and try again.\n";
    	}
    	System.out.println("Capability Token successfully issued.\n");
    	return token ;
    }
    
    public CapabilityToken processTokenTango(String req) {
    	
    	//Send request to PEP for issuing the Capability token -> change the PEP treatment 
    	String requestJson=gson.toJson(req);
    	CapabilityToken ct=pep.sendToken(requestJson);
    	return ct;
    }
    
    
    @PostMapping("/access-with-token")
    public String accessWithToken(@RequestBody AccessRequest request) {
    	
    	//Verify capability token
    	String requestJson=gson.toJson(request);
    	String response=pep.validateCapabilityToken(requestJson);
    	
    	return response;
    }
}

