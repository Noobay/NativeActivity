package com.lunarsong.android;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.inputmethod.InputMethodManager;

public class NativeSurfaceView extends SurfaceView implements SurfaceHolder.Callback
{
	public static boolean mLogEnabled = true;
	
	private NativeThread mNativeThread;
	private final WeakReference< NativeSurfaceView > mThisWeakRef =
            new WeakReference< NativeSurfaceView >(this);
	
	/////////////////////////////////////////////////////////
	//                    Constructors                     //
	/////////////////////////////////////////////////////////
	public NativeSurfaceView( Context context ) 
	{
		super( context );
		
		Init( context );
	}
	
	public NativeSurfaceView( Context context, AttributeSet attrs ) 
	{
        super( context, attrs );
        Init( context );
    }
	
	@Override
    protected void finalize() throws Throwable 
    {
        try 
        {
            if ( mNativeThread != null ) 
            {
                // Close the native thread
            	mNativeThread.requestExitAndWait();
            }
        } 
        finally 
        {
            super.finalize();
        }
    }
	
	/////////////////////////////////////////////////////////
	//                   Initialization                    //
	/////////////////////////////////////////////////////////
	private void Init( Context context )
	{
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed
		SurfaceHolder holder = getHolder();
		holder.addCallback( this );
	
		// Retrieve application's name
		int stringId = context.getApplicationInfo().labelRes;
	    String strAppName = context.getString(stringId);
	    
	    // Start the NativeThread
		mNativeThread = new NativeThread( strAppName, mThisWeakRef );
		mNativeThread.start();
	}


	/////////////////////////////////////////////////////////
	//            SurfaceHolder Implementations            //
	/////////////////////////////////////////////////////////
	@Override
	public void surfaceChanged( SurfaceHolder holder, int iFormat, int iWidth, int iHeight ) 
	{
		class SurfaceChangedMessage implements Runnable
	    {
	        private int mFormat;
	        private int mWidth;
	        private int mHeight;
	        private NativeThread mNativeThread;
	        public SurfaceChangedMessage( NativeThread nativeThread, int iFormat, int iWidth, int iHeight )
	        { 
	        	mNativeThread = nativeThread;
	        	mFormat = iFormat;
	        	mWidth = iWidth;
	        	mHeight = iHeight;
	        }
	 
	        @Override
	        public void run() 
	        {
	        	mNativeThread.onSurfaceChanged( mFormat, mWidth, mHeight );               
	        }	        
	    }
		
		queueEvent( new SurfaceChangedMessage( mNativeThread, iFormat, iWidth, iHeight ) );		
	}

	@Override
	public void surfaceCreated( SurfaceHolder holder ) 
	{		
		class SurfaceMessage implements Runnable
	    {
	        private Surface mSurface;
	        private NativeThread mNativeThread;
	        public SurfaceMessage( NativeThread nativeThread, Surface surface ) { mNativeThread = nativeThread; mSurface = surface; }
	 
	        @Override
	        public void run() 
	        {
	        	mNativeThread.onSurfaceCreated( mSurface );               
	        }	        
	    }
		
		queueEvent( new SurfaceMessage( mNativeThread, holder.getSurface() ) );
	}

	@Override
	public void surfaceDestroyed( SurfaceHolder holder ) 
	{		
		queueEvent( new Runnable() 
        {
            @Override
            public void run() 
            {
                mNativeThread.onSurfaceDestroyed();
            }
        });
	}
	
	/////////////////////////////////////////////////////////
	//                  Activity Callbacks                 //
	/////////////////////////////////////////////////////////
	public void onDestroy() 
    {
        mNativeThread.onDestroy();
    }
	
	/**
     * Inform the view that the activity is paused. The owner of this view must
     * call this method when the activity is paused. Calling this method will
     * pause the rendering thread.
     */
    public void onPause() 
    {
    	queueEvent( new Runnable() 
        {
            @Override
            public void run() 
            {
                mNativeThread.onPause();
            }
        });
    }

    /**
     * Inform the view that the activity is resumed. The owner of this view must
     * call this method when the activity is resumed.
     */
    public void onResume() 
    {
    	queueEvent( new Runnable() 
        {
            @Override
            public void run() 
            {
                mNativeThread.onResume();
            }
        });
    }
    
    /**
     * This method is used as part of the View class.
     */
    @Override
    protected void onAttachedToWindow() 
    {
    	// Call super
        super.onAttachedToWindow();

        // Handle attachment
        queueEvent( new Runnable() 
        {
            @Override
            public void run() 
            {
                mNativeThread.onAttachedToWindow();
            }
        });
    }

    /**
     * This method is used as part of the View class.
     */
    @Override
    protected void onDetachedFromWindow() 
    {
    	// Handle detachment
    	queueEvent( new Runnable() 
        {
            @Override
            public void run() 
            {
                mNativeThread.onDetachedFromWindow();
            }
        });
    	
    	// call super
        super.onDetachedFromWindow();
    }
    
    @Override
    public void onWindowFocusChanged( boolean hasWindowFocus )
    {
    	super.onWindowFocusChanged( hasWindowFocus );
    	
    	class WindowFocusMessage implements Runnable
	    {
	        private NativeThread mNativeThread;
	        private boolean mHasFocus;
	        public WindowFocusMessage( NativeThread nativeThread, boolean hasWindowFocus ) { mNativeThread = nativeThread; mHasFocus = hasWindowFocus; }
	 
	        @Override
	        public void run() 
	        {
	        	mNativeThread.onWindowFocusChanged( mHasFocus );               
	        }	        
	    }
		
		queueEvent( new WindowFocusMessage( mNativeThread, hasWindowFocus ) );
    }
    
    @Override
    public boolean onTouchEvent( MotionEvent event ) 
    {
        // Implement Runnable for MotionEvent parameter
        class MotionEventRunnable implements Runnable
        {
            private MotionEvent mEvent;
            private NativeThread mNativeThread;
            public MotionEventRunnable( NativeThread nativeThread, MotionEvent event ) { mNativeThread = nativeThread; mEvent = event; }
     
            @Override
            public void run() 
            {
                // Get the number of pointers to iterate
                int iNumPointers = mEvent.getPointerCount();
                for ( int i = 0; i < iNumPointers; ++i )
                {
                    // Get the pointer ID and index
                    int iPointerID = mEvent.getPointerId( i );
                    int iPointerIndex = mEvent.findPointerIndex( iPointerID );
     
                    // Get the xy position and action
                    float x = mEvent.getX( iPointerIndex );
                    float y = mEvent.getY( iPointerIndex );
     
                    int iAction = mEvent.getActionMasked();
     
                    // Send to C++
                    mNativeThread.onTouch( iPointerID, x, y, iAction );
                }               
            }
        }
     
        // Send the event to the renderer thread
        queueEvent( new MotionEventRunnable( mNativeThread, event ) );
     
        return true;
    }
    
    @Override
    public boolean onKeyUp( int iKeyCode, KeyEvent event ) 
    {
        class KeyEventMessage implements Runnable
        {
            private KeyEvent mEvent;
            private int mKeyCode;
            private NativeThread mNativeThread;
            public KeyEventMessage( NativeThread nativeThread, int iKeyCode, KeyEvent event ) { mNativeThread = nativeThread; mKeyCode = iKeyCode; mEvent = event; }
     
            @Override
            public void run() 
            {
            	mNativeThread.onKeyUp( mKeyCode, mEvent );
            }
        }
     
        // Send the event to the renderer thread
        queueEvent( new KeyEventMessage( mNativeThread, iKeyCode, event ) );
    	
    	return true;
    }
    
    /**
     * Queue a runnable to be run on the GL rendering thread. This can be used
     * to communicate with the Renderer on the rendering thread.
     * Must not be called before a renderer has been set.
     * @param r the runnable to be run on the GL rendering thread.
     */
    public void queueEvent(Runnable r) 
    {
    	mNativeThread.queueEvent(r);
    }
    
	/////////////////////////////////////////////////////////
	//                 class NativeThread                  //
	/////////////////////////////////////////////////////////
	@SuppressLint("MissingSuperCall")
	public static class NativeThread extends Thread
	{
		String mApplicationName;
		
		private boolean mExited = false;
		
		private ArrayList<Runnable> mEventQueue = new ArrayList<Runnable>();
		private ArrayList<NativeMessage> mMessageQueue = new ArrayList<NativeMessage>();
		private WeakReference<NativeSurfaceView> mNativeSurfaceViewWeakRef;
		
		// Focus and rendering related
		private boolean mPaused = true;
		private boolean mDetached = true;
		private boolean mLostFocus = true;
		private boolean mHasSurface = false;
		
		private boolean mIsVisible = false;
		
		public boolean isVisible()
		{
			if ( mPaused == false && mDetached == false && mLostFocus == false && mHasSurface == true )
			{
				return true;
			}
			
			return false;
		}

		public void onSurfaceDestroyed() 
		{			
			mHasSurface = false;
			handleVisibility();
			
			queueMessage( new NativeMessage( NativeMessage.NativeEventType.SurfaceDestroyed ) );
		}

		public void onSurfaceCreated( Surface surface ) 
		{
			NativeMessage message = new NativeMessage( NativeMessage.NativeEventType.SurfaceCreated );
			message.mSurface = surface;
			queueMessage( message );
			
			mHasSurface = true;
			handleVisibility();			
		}
		
		public void onSurfaceChanged( int iFormat, int iWidth, int iHeight ) 
		{
			NativeSurfaceView pNativeSurfaceView = mNativeSurfaceViewWeakRef.get();
			if ( pNativeSurfaceView != null )
			{				
				pNativeSurfaceView.nativeOnSurfaceChanged( iFormat, iWidth, iHeight );
			}
		}

		private void handleVisibility()
		{
			if ( mIsVisible )
			{
				if ( isVisible() == false )
				{
					// Handle visibility
					queueMessage( new NativeMessage( NativeMessage.NativeEventType.WindowHidden ) );
					
					// Save new state
					mIsVisible = false;
				}
			}
			
			else
			{
				if ( isVisible() == true )
				{
					// Handle visibility
					queueMessage( new NativeMessage( NativeMessage.NativeEventType.WindowVisible ) );
					
					// Save new state
					mIsVisible = true;
				}
			}						
		}
		
		NativeThread( String applicationName, WeakReference<NativeSurfaceView> pNativeSurfaceView )
		{
			super();
			
			mApplicationName = applicationName;
			
			mNativeSurfaceViewWeakRef = pNativeSurfaceView;
		}
		
		@Override
	    public void run() 
		{
			// Thread started
			setName( "NativeThread " + getId() );            
			
			// Run native main loop
			NativeSurfaceView pNativeSurfaceView = mNativeSurfaceViewWeakRef.get();
			if ( pNativeSurfaceView != null )
			{				
				pNativeSurfaceView.nativeMain( mApplicationName );
			}
			
			// Exit
			synchronized ( this )
			{
				mExited = true;
				notifyAll();
			}
		}
		
		public void onDestroy() 
	    {
			if ( mLogEnabled )
			{
				Log.d( "NativeActivity", "onDestroy called" );
			}
			
			// Call thread's exit and wait
			requestExitAndWait();
	    }
		
		public void onPause() 
	    {
			if ( mLogEnabled )
			{
				Log.d( "NativeActivity", "onPause called" );
			}
			
			mPaused = true;
			queueMessage( new NativeMessage( NativeMessage.NativeEventType.ApplicationPaused ) );
			handleVisibility();
	    }

	    public void onResume() 
	    {
	    	if ( mLogEnabled )
	    	{
	    		Log.d( "NativeActivity", "onResume called" );
	    	}
	    	
	    	mPaused = false;
	    	queueMessage( new NativeMessage( NativeMessage.NativeEventType.ApplicationResumed ) );
	    	handleVisibility();
	    	
	    }

	    public void onWindowFocusChanged( boolean hasWindowFocus ) 
		{
	    	mLostFocus = !hasWindowFocus;
	    	handleVisibility();
		}
	    
	    public void onAttachedToWindow() 
	    {
	    	if ( mLogEnabled )
	    	{
	    		Log.d( "NativeActivity", "onAttachedToWindow called" );
	    	}
	    	
	    	mDetached = false;
	    	handleVisibility();
	    }

	    
	    public void onDetachedFromWindow() 
	    {
	    	if ( mLogEnabled )
	    	{
	    		Log.d( "NativeActivity", "onDetachedFromWindow called" );
	    	}
	    	
	    	mDetached = true;
	    	handleVisibility();
	    }
		
		public void requestExitAndWait() 
		{
			// Send shutdown event
			queueEvent( new NativeMessage( NativeMessage.NativeEventType.ApplicationShutdown ) );
			
			// Wait for thread to terminate
	        synchronized( this ) 
	        {
	            while ( !mExited )
	            {
	                try 
	                {
	                    wait();
	                } 
	                
	                catch ( InterruptedException ex ) 
	                {
	                    Thread.currentThread().interrupt();
	                }
	            }
	        }
	    }

		/////////////////////////////////////////////////////////
		//                        Input                       //
		/////////////////////////////////////////////////////////
		public void onTouch( int iIndex, float fPosX, float fPosY, int iAction )
		{
			NativeSurfaceView pNativeSurfaceView = mNativeSurfaceViewWeakRef.get();
			if ( pNativeSurfaceView != null )
			{				
				pNativeSurfaceView.nativeOnTouch( iIndex, fPosX, fPosY, iAction );
			}
		}
		
		public void onKeyUp( int iKeyCode, KeyEvent event ) 
		{
			NativeSurfaceView pNativeSurfaceView = mNativeSurfaceViewWeakRef.get();
			if ( pNativeSurfaceView != null )
			{				
				pNativeSurfaceView.nativeOnKeyUp( iKeyCode, event.getUnicodeChar() );
			}			
		}
		
		/////////////////////////////////////////////////////////
		//                        Events                       //
		/////////////////////////////////////////////////////////
		
		/**
		 * @param r
		 */
		public void queueEvent( Runnable r ) 
		{
            if ( r == null ) 
            {
                throw new IllegalArgumentException( "r must not be null" );
            }
            
            synchronized( mEventQueue ) 
            {
                mEventQueue.add( r );
            }
        }
		
		/**
		 * @param message
		 */
		public void queueEvent( NativeMessage message ) 
		{
            if ( message == null ) 
            {
                throw new IllegalArgumentException( "[queueEvent]: message must not be null" );
            }
            
            synchronized( mEventQueue ) 
            {
                mEventQueue.add( new MessageRunnable( this, message ) );
            }
        }
		
		/**
		 * 
		 */
		public void handleEvents()
		{
			synchronized( mEventQueue )
			{
				while ( !mEventQueue.isEmpty() )
				{
					Runnable r = mEventQueue.remove( 0 );
					r.run();
				}
			}
		}
		
		/////////////////////////////////////////////////////////
		//                       Messages                      //
		/////////////////////////////////////////////////////////
		
		/**
		 * 
		 * Must be called from within the thread!
		 * 
		 * @param message
		 */
		public void queueMessage( NativeMessage message ) 
		{
            if ( message == null ) 
            {
                throw new IllegalArgumentException( "message must not be null" );
            }
            
            //synchronized( mMessageQueue ) 
            {
            	mMessageQueue.add( message );
            }
        }
		
		/**
		 * @return
		 */
		public NativeMessage peekMessage()
		{
			//synchronized( mMessageQueue )
			{
				if ( !mMessageQueue.isEmpty() )
				{
					return mMessageQueue.remove( 0 );
				}
				
				return null;
			}
		}
		
		/**
		 * Helper runnable which queues a message
		 * into the NativeThread queue
		 */
		private class MessageRunnable implements Runnable
	    {
	        private NativeMessage mMessage;
	        private NativeThread  mNativeThread;
	        /**
	         * @param thread - the NativeThread
	         * @param message - the Message to queue
	         */
	        public MessageRunnable( NativeThread thread, NativeMessage message ) { mMessage = message; mNativeThread = thread; }
	 
	        /* 
	         * Queues the messages to the NativeThread
	         */
	        @Override
	        public void run() 
	        {
	            mNativeThread.queueMessage( mMessage );               
	        }
	    }
	}
	
	/////////////////////////////////////////////////////////
	//                        Native                       //
	/////////////////////////////////////////////////////////

	// Methods to be called from native
	// must be called from NativeThread!
	NativeMessage peekMessage()
	{
		NativeMessage message = mNativeThread.peekMessage();
		
		return message;
	}

	void pollMessages()
	{
		mNativeThread.handleEvents();
	}
	
	void showKeyboard()
	{
		if ( mLogEnabled )
		{
			Log.d( "NativeActivity", "[Native]: Show keyboard." );
		}
		
		if ( requestFocus() ) 
    	{
            InputMethodManager imm = (InputMethodManager)getContext().getSystemService( Context.INPUT_METHOD_SERVICE );
            imm.showSoftInput( this, InputMethodManager.SHOW_IMPLICIT );
        }
	}
	
	void hideKeyboard()
	{
		if ( mLogEnabled )
		{
			Log.d( "NativeActivity", "[Native]: Hide keyboard." );
		}
		
		InputMethodManager imm = (InputMethodManager)getContext().getSystemService( Context.INPUT_METHOD_SERVICE );
        imm.hideSoftInputFromWindow( getWindowToken(), 0 );
	}
	
	/* Native methods */
	public native void nativeMain( String strApplicationName );
	
	// Surface
	public native void nativeOnSurfaceChanged( int iFormat, int iWidth, int iHeight );
	
	// Input
	public native void nativeOnTouch( int iIndex, float fPosX, float fPosY, int iAction );
	public native void nativeOnKeyUp( int iKeyCode, int iUnicodeChar );
	
	// Load native library
	static
	{
		System.loadLibrary( "NativeActivityRuntime" );
	}
}
