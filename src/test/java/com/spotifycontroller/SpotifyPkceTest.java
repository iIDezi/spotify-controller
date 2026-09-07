package com.spotifycontroller;

import java.security.NoSuchAlgorithmException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpotifyPkceTest
{
	@Test
	public void createsRfc7636CodeChallenge() throws NoSuchAlgorithmException
	{
		String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

		assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", SpotifyApiClient.codeChallenge(verifier));
	}
}
