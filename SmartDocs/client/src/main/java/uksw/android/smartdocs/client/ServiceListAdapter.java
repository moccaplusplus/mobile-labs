package uksw.android.smartdocs.client;

import android.net.nsd.NsdServiceInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ServiceListAdapter extends RecyclerView.Adapter<ServiceListAdapter.ViewHolderImpl> {
    public static class ViewHolderImpl extends RecyclerView.ViewHolder {
        private final TextView serviceNameView;

        public ViewHolderImpl(View view) {
            super(view);
            serviceNameView = view.findViewById(R.id.service_name);
        }

        public void setService(NsdServiceInfo serviceInfo) {
            serviceNameView.setText(serviceInfo.getServiceName());
        }
    }

    private final List<NsdServiceInfo> serviceList = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolderImpl onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.service_info_item, parent, false);
        return new ViewHolderImpl(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderImpl holder, int position) {
        holder.setService(serviceList.get(position));
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    public void addServiceInfo(NsdServiceInfo serviceInfo) {
        int index = indexOf(serviceInfo);
        if (index == -1) {
            index = serviceList.size();
            serviceList.add(serviceInfo);
            notifyItemInserted(index);
        } else {
            serviceList.set(index, serviceInfo);
            notifyItemChanged(index);
        }
    }

    public void removeServiceInfo(NsdServiceInfo serviceInfo) {
        int index = indexOf(serviceInfo);
        if (index != -1) {
            serviceList.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void clear() {
        int size = serviceList.size();
        serviceList.clear();
        notifyItemRangeRemoved(0, size);
    }

    private int indexOf(NsdServiceInfo serviceInfo) {
        for (int i = 0; i < serviceList.size(); ++i) {
            NsdServiceInfo item = serviceList.get(i);
            if (item.getServiceName().equals(serviceInfo.getServiceName())) {
                return i;
            }
        }
        return -1;
    }
}
