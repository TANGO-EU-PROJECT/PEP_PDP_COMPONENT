package com.example.demo.PAP;

import java.util.ArrayList;

import com.example.demo.models.Policy;

public interface PAPInterface {
	ArrayList<Policy> getPolicies(String didSP, String recursoSolicitado, String action);
	public void addPolicy( Policy policy, String resource);
}
