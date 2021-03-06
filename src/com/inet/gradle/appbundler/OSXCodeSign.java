package com.inet.gradle.appbundler;

import java.io.File;
import java.util.ArrayList;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.abstracts.AbstractBuilder;
import com.inet.gradle.setup.abstracts.AbstractSetupBuilder;
import com.inet.gradle.setup.abstracts.AbstractTask;

/**
 * Create code signature for packages. Deep Signing. 
 * @author gamma
 * @param <T> concrete Task
 * @param <S> concrete SetupBuilder
 *
 */
public class OSXCodeSign<T extends AbstractTask, S extends AbstractSetupBuilder> extends AbstractBuilder<T,S> {

	private String identity, identifier, keychain, keychainPassword;
	private boolean ignoreError, deepsign = true;
	
	/**
	 * Setup up the Sign Tool
	 * @param task task
	 * @param fileResolver resolver
	 */
	public OSXCodeSign(T task, FileResolver fileResolver) {
		super(task, fileResolver);
	}

	/**
	 * Return the Identity to sign with
	 * This is the "Common Name" part from the certificate
	 * @return identity
	 */
	public String getIdentity() {
		if ( identity == null ) {
			throw new IllegalArgumentException( "You have to define the signing identity" );
		}
		return identity;
	}

	/**
	 * Set the Identity to sign with.
	 * This is the "Common Name" part from the certificate
	 * @param identity to sign with
	 */
	public void setIdentity(String identity) {
		this.identity = identity;
	}

	/**
	 * Specific Identifier to embed in code (option -i) 
	 * @return identifier 
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Specific Identifier to embed in code (option -i) 
	 * @param identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Key chain to use for signing. It has to be unlocked.
	 * @return key chain
	 */
	public String getKeychain() {
		return keychain;
	}

	/**
	 * Set Key chain to use for signing. It has to be unlocked. 
	 * @param keychain key chain
	 */
	public void setKeychain(String keychain) {
		this.keychain = keychain;
	}

	/**
	 * The password to unlock the keychain
	 * @return the keychainPassword
	 */
	public String getKeychainPassword() {
		return keychainPassword;
	}

	/**
	 * Set the keychain password to unlock the keychain
	 * @param keychainPassword the keychainPassword to set
	 */
	public void setKeychainPassword(String keychainPassword) {
		this.keychainPassword = keychainPassword;
	}

	/**
	 * True if errors during signing should be ignored
	 * @return ignore errors
	 */
	public boolean isIgnoreError() {
		return ignoreError;
	}

	/**
	 * Should errors be ignored during signing
	 * @param ignoreError ignore
	 */
	public void setIgnoreError(boolean ignoreError) {
		this.ignoreError = ignoreError;
	}

	/**
	 * Unlocks the keychain if the password is not null.
	 * Will unlock the default login.keychain if no other is set.
	 */
	private void unlockKeychain() {
		if ( getKeychainPassword() == null ) {
			return;
		}
		
		String keychain = getKeychain() != null ? getKeychain() : System.getenv("HOME") + "/Library/Keychains/login.keychain";

		// unlock keychain
		ArrayList<String> command = new ArrayList<>();
        command.add( "security" );
        command.add( "-v" );
        command.add( "unlock-keychain" );
        command.add( "-p" );
        command.add( getKeychainPassword() );
        command.add( keychain );

        exec( command, null, null, isIgnoreError() );			
	}
	
	/**
	 * Signed an application package
	 * @param path of the application
	 */
	public void signApplication( File path ) {
		
		unlockKeychain();
		
		// Codesign
		ArrayList<String> command = new ArrayList<>();
        command.add( "codesign" );
        command.add( "-f" );
        
        if ( isDeepsign() ) {
        	command.add( "--deep" );
        }
        
        command.add( "-s" );
        command.add( getIdentity() );
        
        if ( getIdentifier() != null ) {
            command.add( "-i" );
            command.add( getIdentifier() );
        }
        
        if ( getKeychain() != null ) {
            command.add( "--keychain" );
            command.add( getKeychain() );
        }

        command.add( path.getAbsolutePath() );
        exec( command, null, null, isIgnoreError() );
	}

	/**
	 * Signed a product package
	 * @param path of the application
	 */
	public void signProduct( File path ) {
		
		unlockKeychain();
		
		// Productsign
		ArrayList<String> command = new ArrayList<>();
        command.add( "productsign" );
        command.add( "--sign" );
        command.add( getIdentity() );
        
        if ( getKeychain() != null ) {
            command.add( "--keychain" );
            command.add( getKeychain() );
        }

        
        command.add( path.getAbsolutePath() );
        
        File output = new File( path.getParentFile(), "signed." + path.getName() ); 
        command.add( output.getAbsolutePath() );
        exec( command, null, null, isIgnoreError() );

        // Move to old directory
        if ( output.exists() && path.delete() ) {
        	output.renameTo( path );
        }
	}

	/**
	 * Should be deep signed?
	 * @return
	 */
	public boolean isDeepsign() {
		return deepsign;
	}

	/**
	 * Set deep signing
	 * @param deepsign deep sign?
	 */
	public void setDeepsign(boolean deepsign) {
		this.deepsign = deepsign;
	}
}
