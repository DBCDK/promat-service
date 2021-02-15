@import dk.dbc.promat.service.persistence.TaskFieldType
@import dk.dbc.promat.service.persistence.PromatTask
@param dk.dbc.promat.service.persistence.PromatCase promatCase


!{var tasks = promatCase.getTasks(); }

@if (tasks != null)
    @for (var task : tasks)
        @if(task.getTaskFieldType() == TaskFieldType.METAKOMPAS)
            Du bedes tildele metadata via <a href="https://metakompas.dk">https://metakompas.dk</a>
        @endif
    @endfor
@endif