package com.mapzen.tangram; public interface MapChangeListener { void onViewComplete(); void onRegionWillChange(boolean a); void onRegionIsChanging(); void onRegionDidChange(boolean a); }
