/** 
    Copyright 2015 Tim Engler, Rareventure LLC

    This file is part of Tiny Travel Tracker.

    Tiny Travel Tracker is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Tiny Travel Tracker is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Tiny Travel Tracker.  If not, see <http://www.gnu.org/licenses/>.

*/
package com.rareventure.gps2.gpx;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.text.InputType;

import com.rareventure.android.DbUtil;
import com.rareventure.android.ProgressDialogActivity;
import com.rareventure.android.Util;
import com.rareventure.gps2.GTG;
import com.rareventure.gps2.GpsTrailerCrypt;
import com.rareventure.gps2.R;
import com.rareventure.gps2.database.GpsLocationRow;
import com.rareventure.gps2.database.TimeZoneTimeRow;
import com.rareventure.gpx.GpxWriter;
import com.rareventure.util.OutputStreamToInputStreamPipe;

import de.idyl.winzipaes.AesZipFileEncrypter;
import de.idyl.winzipaes.impl.AESEncrypterBC;

public class CreateGpxBackup extends ProgressDialogActivity {
	
	private EditText enterPasswordView;
	private CheckBox passwordCheckBox;
	private EditText reenterPasswordView;
	private EditText filenameView;
	private boolean isShowBackupFinishedOnResume;
    private boolean isShowBackupFailedOnResume;
	private boolean isPause = true;
	private String filePath;
    private static final int REQUEST_CREATE_DOCUMENT = 14;
    private PendingBackupRequest pendingRequest;

	@Override
	public void doOnCreate(Bundle b) {
		super.doOnCreate(b);

		setContentView(R.layout.create_gpx_backup);
        
        enterPasswordView = (EditText)findViewById(R.id.enter_new_password);
        reenterPasswordView = (EditText)findViewById(R.id.reenter_new_password);
        
        passwordCheckBox = (CheckBox)findViewById(R.id.passwordCheckBox);
        
        
        filenameView = (EditText)findViewById(R.id.filename);
        enterPasswordView.setSaveEnabled(false);
        reenterPasswordView.setSaveEnabled(false);
        pendingRequest = PendingBackupRequest.restore(b);
	}
	
    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (pendingRequest != null) pendingRequest.save(state);
    }

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private Task backupTask;
	protected static final int MAX_BUFFER_SIZE = 65536;
	
	/**
	 * Number of processed rows before we update the progress bar
	 */
	protected static final int UPDATE_INCREMENT = 250;
	
	@Override
	public void doOnResume() {
		super.doOnResume();
		
		isPause = false;
		showCreateBackupFinished();
        if (isShowBackupFailedOnResume) showWriteError();
		
		updatePasswordViews();
		
        if (filenameView.getText().length() == 0)
            filenameView.setText("TTT-"+DATE_FORMAT.format(new Date())+".gpx.zip");
	}
	
	@Override
	public void doOnPause(boolean doOnResumeCalled)
	{
		super.doOnPause(doOnResumeCalled);
		
		isPause=true;
	}
	
	@Override
	public void finish()
	{
		//if they try and enter a password and hit cancel, GTGActivityHelper has no choice but to back up through all the activities on the stack
		//so we have to cancel the restore, because our activity is about to finished.
		//Note that using singleTask doesn't help here because we are finishing the task regardless.
		if(backupTask != null)
			backupTask.cancel();
		backupTask = null;
		
		super.finish();
	}

    @Override
    protected void onDestroy() {
        if (backupTask != null) backupTask.cancel();
        super.onDestroy();
    }


	public void onCreateButton(final View v)
	{
        if (backupTask != null) return;
		final boolean passwordProtected = passwordCheckBox.isChecked();
		final String password = enterPasswordView.getText().toString(); 
		
		if(passwordProtected)
		{
			if(!password.equals(reenterPasswordView.getText().toString()))
			{
		        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.error)
                .setMessage(R.string.passwords_dont_match)
                .setNeutralButton(R.string.details_ok, null)
                .show();
				return;
			}
		}
		
		String localFilename = filenameView.getText().toString();
		
		if(!localFilename.toLowerCase().matches(".*\\.zip"))
		{
			localFilename += ".zip";
		}
		
		final String filename = localFilename;
		
        pendingRequest = new PendingBackupRequest(filename, passwordProtected, password);
        startInternalActivityForResult(BackupDocuments.create(filename), REQUEST_CREATE_DOCUMENT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CREATE_DOCUMENT) return;
        final Uri destination = BackupDocuments.selected(resultCode, data);
        final PendingBackupRequest request = pendingRequest;
        pendingRequest = null;
        if (destination == null) return;
        if (request == null) {
            showWriteError();
            return;
        }
        if (request.needsPassword()) {
            // Never save a backup password in system state or silently export without encryption.
            final EditText password = new EditText(this);
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            password.setSaveEnabled(false);
            new AlertDialog.Builder(this).setTitle(R.string.enter_backup_password)
                    .setView(password).setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.details_ok, new OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            createBackup(destination, request.filename, true, password.getText().toString());
                        }
                    }).show();
            return;
        }
        createBackup(destination, request.filename, request.passwordProtected, request.password);
    }

    private void createBackup(final Uri destination, final String filename,
            final boolean passwordProtected, final String password) {
        final File archive;
        try {
            archive = File.createTempFile("gpx-export-", ".zip", getCacheDir());
        } catch (IOException e) {
            showWriteError();
            return;
        }
        filePath = BackupDocuments.displayName(getContentResolver(), destination);
        findViewById(R.id.createButton).setEnabled(false);

		runLongTask(backupTask = new Task() {
			
			private volatile IOException failedException;
            private Thread zipThread;
            private final BackupCancellation cancellation = new BackupCancellation();

            @Override public void cancel() {
                isCanceled = true;
                cancellation.cancel();
            }
			
			@Override
			public void doIt() {
				final OutputStreamToInputStreamPipe bis = new OutputStreamToInputStreamPipe(1024, 65536);
				
				final GpxWriter gis = new GpxWriter(bis);
				
				if(!passwordProtected)
				{
					if(!createRegularZipOutputThread(archive.getPath(),  BackupDocuments.entryName(filename), bis))
						return;
					
				}
				else
				{
					if(!createEncryptedZipOutputThread(archive.getPath(),  BackupDocuments.entryName(filename), bis, password))
						return;
				}
				
				int total = GTG.gpsLocCache.getNextRowId();
				
				super.setMaxProgress(total);
				
				//read the data
				Cursor c = null;
				Iterator<TimeZoneTimeRow> tztri = GTG.tztSet.getIterator();
				
				// this try is to close the transaction with a finally
				//and for removing the rwtm locks
				GTG.cacheCreatorLock.registerReadingThread();

				try {
					/* ttt_installer:remove_line */Log.d(GTG.TAG,"Started gpx writing...");
					
					c = GTG.gpsLocDbAccessor.query( null, "_id", (String [])null);
			
					GpsLocationRow currGpsLocRow = GpsTrailerCrypt.allocateGpsLocationRow();

					TimeZoneTimeRow tztRow, lastTztRow = null;
					
					if(tztri.hasNext())
					{
						tztRow = tztri.next();
					}
					else tztRow = null;

					try {
						gis.startDoc(GTG.APP_NAME, 
								getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
					} catch (NameNotFoundException e1) {
						throw new IllegalStateException(e1);
					}
					
					gis.startTrack(filename);
					gis.startSegment();

					// while processing this query
					while (c.moveToNext() && !cancellation.isCancelled()) {
						if(failedException != null)
						{
							return;
						}
							
						int curId = c.getInt(0);
						
						try {
							GTG.gpsLocDbAccessor.readRow(currGpsLocRow, c);
						} catch (Exception e) {
							// sometimes the encryption fails to work (not often)
							// TODO 3: figure out how to fail gracefully in these
							// situations
							Log.e("GTG", "Error reading row " + c.getInt(0)
									+ ", skipping", e);
							
							continue;
						}
						
						
						//add timezone info if this the first gps row that has 
						//a new timezone
						TimeZoneTimeRow tzToAdd;
						
						//if we are now affected by the next timezone row
						if(tztRow != null && currGpsLocRow.getTime() >= tztRow.getTime()) 
						{
							if(tztRow != null && 
									(lastTztRow == null || lastTztRow.getTimeZone() !=
												tztRow.getTimeZone() 
										|| !lastTztRow.getTimeZone().getID().
												equals(tztRow.getTimeZone().getID())))
								tzToAdd = tztRow;
							else
								tzToAdd = null;
							
							if(tztri.hasNext())
							{
								lastTztRow = tztRow;
								tztRow = tztri.next();
							}
							else tztRow = null;
							
						}
						else
							tzToAdd = null;
						
						gis.addPoint(currGpsLocRow.getLatm()/1000000d, currGpsLocRow.getLonm()/1000000d,
								currGpsLocRow.getAltitude(), currGpsLocRow.getTime(), tzToAdd);
						
						if(curId % UPDATE_INCREMENT == 0)
							super.updateProgress(0, total, curId);
						
					} //while processing points
					
					gis.endSegment();
					gis.endTrack();
					gis.endDoc();
					
					//wait for the thread to finish copying the data
                    bis.blockUntilClosed();
                    try {
                        zipThread.join(); // Closing writes the ZIP central directory and AES authentication.
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failedException = new IOException("Interrupted publishing backup", e);
                    }
					
				}
                catch (RuntimeException e) {
                    failedException = new IOException("Unable to create backup", e);
                }
                finally {
                    bis.finishWriting();
                    try { zipThread.join(); }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failedException = new IOException("Interrupted closing backup", e);
                    }
					DbUtil.closeCursors(c);
					
                    GTG.cacheCreatorLock.unregisterReadingThread();
                }
                // A cloud provider may block. Never hold database/cache locks while publishing.
                if (!cancellation.isCancelled() && failedException == null) {
                    try {
                        BackupDocumentStreams.publish(archive,
                                getContentResolver().openOutputStream(destination, "wt"), cancellation);
                    } catch (IOException | SecurityException e) {
                        failedException = new IOException("Unable to publish backup", e);
                    }
                }
            }
	
			private boolean createEncryptedZipOutputThread(String filePath,
					final String zipEntryName, final OutputStreamToInputStreamPipe bis,
					final String password) {
				final AesZipFileEncrypter zfe;
				
				try {
					
					//note, we use AESEncrypterBC because AESEncrypterJCA seems to work
					//but the resulting file is not decryptable by 7z (and possibly winzip)
					//this means we need to include the huge BouncyCastle lib, but I don't
					//see another way, really. Also bouncy castle could standardize encryption
					// and decryption so we don't have to worry about funky phones with weird
					// implementations of java crypto
					zfe = new AesZipFileEncrypter(filePath, new AESEncrypterBC());
				} catch (IOException e) {
					failedException = e;
					return false;
				}
				
				//we copy the data in another thread for speed
				zipThread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						try {
							//this will write the entire stream to os
							zfe.add(zipEntryName, bis, password);
						} catch (IOException | RuntimeException writingException) {
                            Log.e(GTG.TAG, "Failed writing archive", writingException);
                            failedException = new IOException("Unable to write archive", writingException);
						}
						bis.close();
						try {
							zfe.close();
						} catch (IOException e) {
							Log.e(GTG.TAG,"Failed closing os", e);
							failedException = e;
						}
					}
				});
                zipThread.start();
				
				return true;
			}

			private boolean createRegularZipOutputThread(String filePath, String zipEntryName, final OutputStreamToInputStreamPipe bis) {
				final OutputStream os;
				
				try {
					os = Util.createZipOutputStream(filePath,zipEntryName);
				}
				catch(IOException e)
				{
					failedException = e;
					return false;
				}

				//we copy the data in another thread for speed
				zipThread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						try {
							//this will write the entire stream to os
							bis.writeTo(os, 4096);
						} catch (IOException | RuntimeException writingException) {
                            Log.e(GTG.TAG, "Failed writing archive", writingException);
                            failedException = new IOException("Unable to write archive", writingException);
						}
						bis.close();
						try {
							os.close();
						} catch (IOException e) {
							Log.e(GTG.TAG,"Failed closing os", e);
							failedException = e;
						}
					}
				});
                zipThread.start();
				
				return true;
				
			}
			
			@Override
			public void doAfterFinish() {
                archive.delete();
                backupTask = null;
                if (isFinishing() || isDestroyed()) return;
                findViewById(R.id.createButton).setEnabled(true);
                if (cancellation.isCancelled()) {
                    if (isPause) return;
                    new AlertDialog.Builder(CreateGpxBackup.this)
                            .setCancelable(false).setTitle(R.string.backup_has_been_canceled)
                            .setNeutralButton(R.string.details_ok, null).show();
                    return;
                }
                if (failedException != null) {
                    showWriteError();
                    return;
                }
                if (!isCanceled) {
                    isShowBackupFinishedOnResume = true;
                    if (!isPause) showCreateBackupFinished();
                }
			}

		}, true, false, R.string.create_gpx_backup_progress_title, R.string.create_gpx_backup_progress_msg);
	}
	

    private void showWriteError() {
        if (isFinishing() || isDestroyed()) return;
        isShowBackupFailedOnResume = isPause;
        if (isPause) return;
        new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string.error)
                .setMessage(R.string.create_gpx_backup_error_cant_write_backup)
                .setNeutralButton(R.string.details_ok, null).show();
    }

	private void showCreateBackupFinished() {
		if(!isShowBackupFinishedOnResume)
			return;
		
		isShowBackupFinishedOnResume = false;
		
		new AlertDialog.Builder(CreateGpxBackup.this)
        .setCancelable(false)
        .setTitle(R.string.success)
        .setMessage(String.format(getText(R.string.create_backup_finished_fmt).toString(), filePath))
        .setNeutralButton(R.string.details_ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CreateGpxBackup.this.finish();
			}
		})
        .show();
	}

	public void onCancelButton(View v)
	{
		finish();
	}
	
	public void onPasswordCheckBox(View v)
	{
		updatePasswordViews();
	}
	
	private void updatePasswordViews()
	{
		findViewById(R.id.password_file_desc).setVisibility(passwordCheckBox.isChecked() ? View.VISIBLE : View.GONE);
		
		enterPasswordView.setText("");
		reenterPasswordView.setText("");
		
		enterPasswordView.setVisibility(passwordCheckBox.isChecked() ? View.VISIBLE : View.GONE);
		reenterPasswordView.setVisibility(passwordCheckBox.isChecked() ? View.VISIBLE : View.GONE);
	}

	@Override
	public int getRequirements() {
		//we use trial expired here because we allow the user to create a backup if trial is expired
		return GTG.REQUIREMENTS_TRIAL_EXPIRED_ACTIVITY;
	}

}
